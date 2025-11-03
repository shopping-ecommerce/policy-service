package iuh.fit.se.entity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name="policy_versions",
        uniqueConstraints=@UniqueConstraint(name="uk_policy_version", columnNames={"policy_id","version"}),
        indexes = {
                @Index(name="idx_version_status", columnList="status"),
                @Index(name="idx_version_start_end", columnList="start_date, end_date"),
                @Index(name="idx_version_policy", columnList="policy_id")
        })
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class PolicyVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name="policy_id", nullable=false, length=36)
    String policyId;

    @Column(nullable=false, length=20)
    String version;               // v1.0, v1.1,...

    @Column(nullable=false, length=20)
    String status;                // DRAFT | PUBLISHED | RETIRED

    @Column(name="start_date")
    LocalDate startDate;

    @Column(name="end_date")
    LocalDate endDate;

    @Lob
    @Column(name="content_md", nullable=false, columnDefinition="LONGTEXT")
    String contentMd;             // luôn markdown lưu DB

    @Lob
    String changeNotes;

    @Column(nullable=false)
    Boolean requireReconsent = Boolean.FALSE;

    @Column(name="pdf_url", length=500)
    String pdfUrl;

    @Column(name="pdf_sha256", length=128)
    String pdfSha256;

    @Column(name="commission_percent", precision=5, scale=2, nullable=false)
    BigDecimal commissionPercent; // 8.00 = 8%

    @Column(length=100)
    String createdBy;

    LocalDateTime createdAt;
    LocalDateTime publishedAt;
    LocalDateTime updatedAt;

    @PrePersist void prePersist() {
        if (this.status == null) this.status = "DRAFT";
        if (this.commissionPercent == null) this.commissionPercent = new BigDecimal("8.00");
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    @PreUpdate void preUpdate() { this.updatedAt = LocalDateTime.now(); }
}
