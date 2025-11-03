package iuh.fit.se.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VersionResp(
        String id,
        String version,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        String contentMd,
        String pdfUrl,
        String pdfSha256,
        BigDecimal commissionPercent
) {}
