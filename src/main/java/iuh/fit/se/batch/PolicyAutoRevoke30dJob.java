// src/main/java/iuh/fit/se/batch/PolicyAutoRevoke30dJob.java
package iuh.fit.se.batch;

import iuh.fit.se.config.PolicyAutoRevokeProperties;
import iuh.fit.se.dto.response.ApiResponse;
import iuh.fit.se.dto.response.SellerResponse;
import iuh.fit.se.entity.Policy;
import iuh.fit.se.entity.PolicyVersion;
import iuh.fit.se.entity.PolicyWithdrawal;
import iuh.fit.se.repository.PolicyConsentRepository;
import iuh.fit.se.repository.PolicyRepository;
import iuh.fit.se.repository.PolicyVersionRepository;
import iuh.fit.se.repository.PolicyWithdrawalRepository;
import iuh.fit.se.repository.httpclient.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyAutoRevoke30dJob {

    private final PolicyRepository policyRepo;
    private final PolicyVersionRepository versionRepo;
    private final PolicyConsentRepository consentRepo;
    private final PolicyWithdrawalRepository withdrawalRepo;
    private final PolicyAutoRevokeProperties props;
    private final UserClient userClient;

    @Scheduled(cron = "#{@policyAutoRevokeProperties.cron}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void run() {
        if (!props.isEnabled()) {
            log.info("[PolicyAutoRevoke30dJob] disabled, skip.");
            return;
        }

        // 1) Lấy policy & version hiện hành
        Policy pol = policyRepo.findByCodeAndLang("SELLER_TOS", "vi").orElse(null);
        if (pol == null || pol.getCurrentVersionId() == null) {
            log.info("[PolicyAutoRevoke30dJob] No policy/currentVersion -> skip.");
            return;
        }
        PolicyVersion cur = versionRepo.findById(pol.getCurrentVersionId()).orElse(null);
        if (cur == null || cur.getStartDate() == null || !Boolean.TRUE.equals(cur.getRequireReconsent())) {
            log.info("[PolicyAutoRevoke30dJob] current version not reconsent-required or missing startDate -> skip.");
            return;
        }

        // 2) Hạn 30 ngày (config)
        LocalDate deadline = cur.getStartDate().plusDays(props.getDays());
        if (LocalDate.now().isBefore(deadline)) {
            log.info("[PolicyAutoRevoke30dJob] Not reached deadline ({}). Skip.", deadline);
            return;
        }

        log.info("[PolicyAutoRevoke30dJob] Start sweep. Version={} startDate={} deadline={}",
                cur.getVersion(), cur.getStartDate(), deadline);

        // 3) Lấy danh sách seller APPROVED từ user-service
        ApiResponse<List<SellerResponse>> resp = userClient.searchSellerApproved();
        List<SellerResponse> approved = (resp == null || resp.getResult() == null) ? List.of() : resp.getResult();

        int revoked = 0;
        LocalDateTime since = cur.getStartDate().atStartOfDay();
        for (SellerResponse s : approved) {
            String sellerId = s.getId();
            if (sellerId == null) continue;

            // 3.1 Đã consent version hiện hành? -> bỏ qua
            if (consentRepo.existsBySellerIdAndPolicyVersionId(sellerId, cur.getId())) continue;

            // 3.2 Đã withdraw kể từ ngày hiệu lực? -> bỏ qua (đã phản hồi)
            if (withdrawalRepo.existsBySellerIdAndRequestedAtAfter(sellerId, since)) continue;

            // 3.3 Huỷ qua user-service (ẩn/quyền xử lý phía user-service)
            userClient.deleteSeller(sellerId, "POLICY_NO_RESPONSE_30D");
            revoked++;

            // 3.4 Log vào policy_withdrawals để trace
            withdrawalRepo.save(PolicyWithdrawal.builder()
                    .sellerId(sellerId)
                    .policyId(pol.getId())
                    .policyVersionId(cur.getId())
                    .reason("POLICY_NO_RESPONSE_30D")
                    .requestedAt(LocalDateTime.now())
                    .effectiveAt(LocalDateTime.now())
                    .ip("SYSTEM")              // batch
                    .userAgent("batch/auto-revoke-30d")
                    .build());
        }

        log.info("[PolicyAutoRevoke30dJob] Done. Revoked={} for version {}", revoked, cur.getVersion());
    }
}
