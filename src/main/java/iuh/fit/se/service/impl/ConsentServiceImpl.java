// src/main/java/iuh/fit/se/service/impl/ConsentServiceImpl.java
package iuh.fit.se.service.impl;

import iuh.fit.se.constants.PolicyConst;
import iuh.fit.se.dto.response.ConsentStatusBySellerResp;
import iuh.fit.se.dto.response.PolicyConsentResp;
import iuh.fit.se.entity.Policy;
import iuh.fit.se.entity.PolicyConsent;
import iuh.fit.se.entity.PolicyVersion;
import iuh.fit.se.entity.PolicyWithdrawal;
import iuh.fit.se.repository.PolicyConsentRepository;
import iuh.fit.se.repository.PolicyRepository;
import iuh.fit.se.repository.PolicyVersionRepository;
import iuh.fit.se.repository.PolicyWithdrawalRepository;
import iuh.fit.se.repository.httpclient.UserClient;
import iuh.fit.se.service.ConsentService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConsentServiceImpl implements ConsentService {

    PolicyRepository policyRepository;
    PolicyVersionRepository policyVersionRepository;
    PolicyConsentRepository policyConsentRepository;
    PolicyWithdrawalRepository withdrawalRepo;
    UserClient userClient;
    private Policy ensureSellerTos() {
        return policyRepository.findByCodeAndLang(PolicyConst.DEFAULT_CODE, PolicyConst.DEFAULT_LANG)
                .orElseThrow(() -> new IllegalStateException("Chưa khởi tạo SELLER_TOS."));
    }

    private PolicyVersion currentEffective(String policyId) {
        return policyVersionRepository.findEffectiveAt(policyId, LocalDate.now())
                .orElseThrow(() -> new IllegalStateException("Chưa có phiên bản SELLER_TOS hiệu lực hôm nay."));
    }

    @Override
    public ConsentStatusBySellerResp getSellerTosStatusBySeller(String sellerId) {
        // validate seller tồn tại (nếu cần bật lại)
//    sellerRepository.findById(sellerId)
//        .orElseThrow(() -> new IllegalArgumentException("Seller không tồn tại: " + sellerId));

        Policy policy = ensureSellerTos();
        PolicyVersion cur = currentEffective(policy.getId());

        PolicyConsent last = policyConsentRepository
                .findTopBySellerIdAndPolicyIdOrderByConsentedAtDesc(sellerId, policy.getId())
                .orElse(null);

        // Seller đã consent đúng version HIỆN TẠI chưa? → Đây là tiêu chí DUY NHẤT
        boolean hasConsented = last != null
                && cur.getId().equals(last.getPolicyVersionId());

        // Luôn cần chấp nhận lại nếu chưa ký đúng version hiện tại
        // (bắt buộc phải đồng ý mỗi khi có version mới)
        boolean needReconsent = !hasConsented;

        return ConsentStatusBySellerResp.builder()
                .policyId(policy.getId())
                .currentVersionId(cur.getId())
                .currentVersion(cur.getVersion())
                .currentStartDate(cur.getStartDate())
                .currentPdfUrl(cur.getPdfUrl())
                .commissionPercent(cur.getCommissionPercent())
                .requireReconsent(cur.getRequireReconsent())  // vẫn trả về giá trị thật của version (có thể dùng để hiển thị thông báo)
                .hasConsented(hasConsented)                  // chính xác: đã ký version hiện tại chưa
                .lastConsentedAt(last == null ? null : last.getConsentedAt())
                .lastPolicyVersionId(last == null ? null : last.getPolicyVersionId())
                .needReconsent(needReconsent)                // luôn true nếu chưa ký version hiện tại
                .build();
    }

    @Transactional
    @Override
    public PolicyConsentResp acceptSellerTos(String sellerId, String ip, String userAgent) {
        // validate seller
//        Seller s = sellerRepository.findById(sellerId)
//                .orElseThrow(() -> new IllegalArgumentException("Seller không tồn tại: " + sellerId));

        Policy pol = ensureSellerTos();
        PolicyVersion cur = currentEffective(pol.getId());

        // Idempotent: nếu đã consent version hiện hành -> trả về luôn
        if (policyConsentRepository.existsBySellerIdAndPolicyVersionId(sellerId, cur.getId())) {
            PolicyConsent existed = policyConsentRepository
                    .findBySellerIdAndPolicyVersionId(sellerId, cur.getId())
                    .orElse(null);

            if (existed != null) {
                return map(existed);
            }
        }

        PolicyConsent c = PolicyConsent.builder()
                .sellerId(sellerId)
//                .userId(sellerId) // nếu Seller có quan hệ User
                .policyId(pol.getId())
                .policyVersionId(cur.getId())
                .method("CHECKBOX")
                .consentedAt(LocalDateTime.now())
                .ip(ip)
                .userAgent(userAgent)
                .snapshotPdfUrl(cur.getPdfUrl())
                .snapshotPdfSha256(cur.getPdfSha256())
                .build();

        c = policyConsentRepository.save(c);

        // Nếu seller đang bị "revocation scheduled" thì có thể HUỶ lịch thu hồi khi họ đã chấp nhận lại (tuỳ chính sách)
        // revokeScheduler.cancelIfAny(sellerId);

        return map(c);
    }

    @Transactional
    @Override
    public void declineSellerTos(String sellerId,String ip, String userAgent) {

        Policy pol = policyRepository.findByCodeAndLang(PolicyConst.DEFAULT_CODE, PolicyConst.DEFAULT_LANG)
                .orElseThrow(() -> new IllegalStateException("Chưa khởi tạo SELLER_TOS."));
        PolicyVersion cur = policyVersionRepository.findEffectiveAt(pol.getId(), LocalDate.now())
                .orElseThrow(() -> new IllegalStateException("Chưa có phiên bản SELLER_TOS hiệu lực."));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockAt = now.plusDays(14);
        userClient.deleteSeller(sellerId,"DECLINE_POLICY");
        // 1) Ghi log withdrawal
        withdrawalRepo.save(PolicyWithdrawal.builder()
                .sellerId(sellerId)
                .policyId(pol.getId())
                .policyVersionId(cur.getId())
                .reason("POLICY_DECLINED" )
                .requestedAt(now)
                .effectiveAt(lockAt)
                .ip(ip)
                .userAgent(userAgent)
                .build());
    }

    private PolicyConsentResp map(PolicyConsent c) {
        return PolicyConsentResp.builder()
                .id(c.getId())
//                .userId(c.getUserId())
                .sellerId(c.getSellerId())
                .policyId(c.getPolicyId())
                .policyVersionId(c.getPolicyVersionId())
                .method(c.getMethod())
                .consentedAt(c.getConsentedAt())
                .ip(c.getIp())
                .userAgent(c.getUserAgent())
                .snapshotPdfUrl(c.getSnapshotPdfUrl())
                .snapshotPdfSha256(c.getSnapshotPdfSha256())
                .build();
    }
}
