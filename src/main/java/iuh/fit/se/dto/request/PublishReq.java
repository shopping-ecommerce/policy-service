package iuh.fit.se.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PublishReq(
        @NotNull LocalDate startDate
) {}