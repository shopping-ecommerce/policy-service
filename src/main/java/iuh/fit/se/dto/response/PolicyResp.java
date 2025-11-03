package iuh.fit.se.dto.response;

public record PolicyResp(
        String id,
        String code,
        String title,
        String lang,
        String currentVersionId
) {}
