// src/main/java/iuh/fit/se/repository/PolicyWithdrawalRepository.java
package iuh.fit.se.repository;

import iuh.fit.se.entity.PolicyWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PolicyWithdrawalRepository extends JpaRepository<PolicyWithdrawal, String> {

    Optional<PolicyWithdrawal> findTopBySellerIdOrderByRequestedAtDesc(String sellerId);

    List<PolicyWithdrawal> findByEffectiveAtLessThanEqual(LocalDateTime at);
    boolean existsBySellerIdAndRequestedAtAfter(String sellerId, LocalDateTime since);
}
