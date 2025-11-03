package iuh.fit.se.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

// PublishNowReq.java
public record PublishNowReq(
        String version,                // optional
        String contentMd,              // optional
        String changeNotes,            // optional
        BigDecimal commissionPercent,  // optional
        Boolean requireReconsent,      // optional
        @NotNull LocalDate startDate   // bắt buộc khi publish
) {}
