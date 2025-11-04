package iuh.fit.se.entity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name="policy_consents",
        indexes = {
                @Index(name="idx_consent_seller", columnList="seller_id"),
                @Index(name="idx_consent_policy_version", columnList="policy_id, policy_version_id"),
                @Index(name="idx_consent_time", columnList="consented_at")
        })
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class PolicyConsent {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name="seller_id", nullable=false, length=64)
    String sellerId;

    @Column(name="policy_id", nullable=false, length=36)
    String policyId;

    @Column(name="policy_version_id", nullable=false, length=36)
    String policyVersionId;

    @Column(name="method", nullable=false, length=20)
    String method;           // luôn CHECKBOX

    @Column(name="consented_at", nullable=false)
    LocalDateTime consentedAt;

    @Column(length=45)
    String ip;

    @Column(name="user_agent", length=512)
    String userAgent;

    @Column(name="snapshot_pdf_url", length=500)
    String snapshotPdfUrl;

    @Column(name="snapshot_pdf_sha256", length=128)
    String snapshotPdfSha256;

    @PrePersist void prePersist() {
        if (this.method == null || this.method.isBlank()) this.method = "CHECKBOX";
        if (this.consentedAt == null) this.consentedAt = LocalDateTime.now();
    }
}
