// src/main/java/iuh/fit/se/repository/PolicyConsentRepository.java
package iuh.fit.se.repository;

import iuh.fit.se.entity.PolicyConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyConsentRepository extends JpaRepository<PolicyConsent, String> {
    boolean existsBySellerIdAndPolicyVersionId(String sellerId, String policyVersionId);
    Optional<PolicyConsent> findTopBySellerIdAndPolicyIdOrderByConsentedAtDesc(String sellerId, String policyId);
}
