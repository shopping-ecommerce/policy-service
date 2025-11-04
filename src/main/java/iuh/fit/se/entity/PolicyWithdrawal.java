// src/main/java/iuh/fit/se/entity/PolicyWithdrawal.java
package iuh.fit.se.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name="policy_withdrawals",
        indexes = {
                @Index(name="idx_pw_seller", columnList="seller_id"),
                @Index(name="idx_pw_policy_version", columnList="policy_id, policy_version_id"),
                @Index(name="idx_pw_time", columnList="requested_at")
        })
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class PolicyWithdrawal {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name="seller_id", nullable=false, length=36)
    String sellerId;

    @Column(name="policy_id", nullable=false, length=36)
    String policyId;

    @Column(name="policy_version_id", nullable=false, length=36)
    String policyVersionId;   // phiên bản tại thời điểm rút

    @Column(name="reason", length=100)
    String reason;            // POLICY_DECLINED | CLOSE_SHOP | OTHER...

    @Column(name="requested_at", nullable=false)
    LocalDateTime requestedAt;

    @Column(name="effective_at", nullable=false)
    LocalDateTime effectiveAt; // now + 14d

    @Column(length=45) String ip;

    @Column(name="user_agent", length=512)
    String userAgent;

    @PrePersist void pre() {
        if (requestedAt == null) requestedAt = LocalDateTime.now();
    }
}
