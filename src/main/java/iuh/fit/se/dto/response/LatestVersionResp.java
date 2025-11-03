package iuh.fit.se.dto.response;

public record LatestVersionResp(
        PolicyResp policy,
        VersionResp version
) {}