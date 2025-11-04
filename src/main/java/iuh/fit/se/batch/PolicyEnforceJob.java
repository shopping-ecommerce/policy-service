package iuh.fit.se.batch;

import iuh.fit.event.dto.PolicyEnforceEvent;
import iuh.fit.se.constants.PolicyConst;
import iuh.fit.se.dto.response.ApiResponse;
import iuh.fit.se.dto.response.PolicyResp;
import iuh.fit.se.dto.response.SellerResponse;
import iuh.fit.se.entity.Policy;
import iuh.fit.se.entity.PolicyVersion;
import iuh.fit.se.entity.PolicyWithdrawal;
import iuh.fit.se.repository.PolicyRepository;
import iuh.fit.se.repository.httpclient.UserClient;
import iuh.fit.se.service.PolicyService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class PolicyEnforceJob {
    KafkaTemplate<String, Object> kafkaTemplate;
    PolicyRepository policyRepository;
    PolicyService policyService;
    UserClient userClient;
    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void run() {
        log.info("[PolicyEnforceJob] Running policy enforce job...");
        LocalDate at = LocalDate.now();
        PolicyResp pol = policyService.getPolicyByCode(PolicyConst.DEFAULT_CODE, PolicyConst.DEFAULT_LANG);
        if (pol == null) { log.warn("No policy found"); return; }

        var ver = policyService.getEffectiveAt(pol.id(), at);
        if (ver == null || ver.startDate() == null) return;

        if (at.equals(ver.startDate())) {
            List<String> emails;
            try {
                emails = userClient.getAllSellerEmails().getResult();
            } catch (Exception ex) {
                log.error("Cannot fetch seller emails: {}", ex.getMessage());
                return;
            }
            kafkaTemplate.send("policy-enforce-topic", PolicyEnforceEvent.builder().emails(emails).build());
            log.info("[PolicyEnforceJob] Event sent for policy {} at {}", pol.code(), at);
        }
    }

}