package iuh.fit.se.dto.response;

public record CreatePolicyWithVersionResp(
        PolicyResp policy,
        VersionResp version
) {}
