package iuh.fit.se.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePolicyWithVersionReq(
        @NotBlank String code,
        @NotBlank String title,
        String version,                 // optional, auto next nếu null/blank
        @NotBlank String contentMd,
        BigDecimal commissionPercent,   // default 8.00 nếu null
        String changeNotes,
        Boolean requireReconsent,
        @NotNull Boolean publishNow,
        LocalDate startDate             // required nếu publishNow = true
) {}
