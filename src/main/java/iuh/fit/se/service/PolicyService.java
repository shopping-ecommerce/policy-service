package iuh.fit.se.service;

import iuh.fit.se.dto.request.CreatePolicyWithVersionReq;
import iuh.fit.se.dto.request.PublishNowReq;
import iuh.fit.se.dto.request.SaveDraftReq;
import iuh.fit.se.dto.response.CreatePolicyWithVersionResp;
import iuh.fit.se.dto.response.PolicyResp;
import iuh.fit.se.dto.response.VersionResp;

import java.time.LocalDate;
import java.util.List;

public interface PolicyService {
    // dùng cho endpoint pin code (seller-tos) khi mở trang soạn thảo lần đầu
    VersionResp getOrCreateDraftByCode(String code, String lang, String defaultTitle, String createdBy);

    VersionResp saveDraft(SaveDraftReq req, String createdBy);

    PolicyResp getPolicyByCode(String code, String lang);

    List<VersionResp> listVersions(String policyId);


    VersionResp getEffectiveAt(String policyId, LocalDate at);

    VersionResp getVersion(String policyId, String version);

    void discardDraft(String policyId);
    VersionResp publishNowForSellerTos(PublishNowReq req, String createdBy) throws Exception;

}
