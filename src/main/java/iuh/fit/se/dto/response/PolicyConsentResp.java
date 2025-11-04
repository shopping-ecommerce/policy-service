// src/main/java/iuh/fit/se/dto/response/PolicyConsentResp.java
package iuh.fit.se.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class PolicyConsentResp {
    String id;
    String sellerId;
    String policyId;
    String policyVersionId;
    String method;
    LocalDateTime consentedAt;
    String ip;
    String userAgent;
    String snapshotPdfUrl;
    String snapshotPdfSha256;
}
