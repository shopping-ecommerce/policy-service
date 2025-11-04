// src/main/java/iuh/fit/se/dto/response/ConsentStatusBySellerResp.java
package iuh.fit.se.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ConsentStatusBySellerResp {
    String policyId;
    String currentVersionId;
    String currentVersion;
    LocalDate currentStartDate;
    String currentPdfUrl;
    BigDecimal commissionPercent;
    Boolean requireReconsent;

    Boolean hasConsented;          // seller đã consent version hiện hành?
    LocalDateTime lastConsentedAt;
    String lastPolicyVersionId;

    Boolean needReconsent;         // để quyết định có bật modal hay không
}
