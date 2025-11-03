package iuh.fit.se.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "policies",
        uniqueConstraints = @UniqueConstraint(name="uk_policy_code_lang", columnNames={"code","lang"}),
        indexes = @Index(name="idx_policy_code", columnList="code"))
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Policy {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable=false, length=100)
    String code;            // SELLER_TOS, FEE_POLICY...

    @Column(nullable=false, length=255)
    String title;

    @Column(nullable=false, length=10)
    String lang;            // luôn "vi"

    @Column(name="current_version_id", length=36)
    String currentVersionId;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @PrePersist void prePersist() {
        if (this.lang == null || this.lang.isBlank()) this.lang = "vi";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    @PreUpdate void preUpdate() {
        if (this.lang == null || this.lang.isBlank()) this.lang = "vi";
        this.updatedAt = LocalDateTime.now();
    }
}
