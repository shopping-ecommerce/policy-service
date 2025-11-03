package iuh.fit.se.controller;

import iuh.fit.se.constants.PolicyConst;
import iuh.fit.se.dto.request.*;
import iuh.fit.se.dto.response.CreatePolicyWithVersionResp;
import iuh.fit.se.dto.response.PolicyResp;
import iuh.fit.se.dto.response.VersionResp;
import iuh.fit.se.service.PolicyService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/policies")
public class PolicyController {

    PolicyService policyService;

    /** Mở trang soạn thảo lần đầu:
     * - Nếu có DRAFT -> trả về
     * - Nếu không có -> clone từ effective today -> nếu không -> clone published mới nhất -> nếu vẫn không thì tạo nháp rỗng
     */
    @GetMapping("/seller-tos/draft")
    public ResponseEntity<VersionResp> getOrCreateSellerTosDraft(
            @RequestHeader(value = "X-User-Id", required = false) String uid
    ) {
        return ResponseEntity.ok(
                policyService.getOrCreateDraftByCode(
                        PolicyConst.DEFAULT_CODE,
                        PolicyConst.DEFAULT_LANG,
                        PolicyConst.DEFAULT_TITLE,
                        uid
                )
        );
    }

    /** Lưu/Tạo bản nháp duy nhất cho SELLER_TOS (body tối giản, không cần code/title) */
    @PostMapping("/seller-tos/draft")
    public ResponseEntity<VersionResp> saveSellerTosDraft(
            @RequestBody SaveDraftBody body,
            @RequestHeader(value = "X-User-Id", required = false) String uid
    ) {
        SaveDraftReq fixed = new SaveDraftReq(
                PolicyConst.DEFAULT_CODE,
                PolicyConst.DEFAULT_LANG,
                body.getVersion(),
                body.getContentMd(),
                body.getChangeNotes(),
                body.getCommissionPercent(),
                body.getRequireReconsent()
        );
        return ResponseEntity.ok(policyService.saveDraft(fixed, uid));
    }




    /** Lịch sử version SELLER_TOS */
    @GetMapping("/seller-tos/versions")
    public ResponseEntity<List<VersionResp>> listSellerTosVersions() {
        PolicyResp pol = policyService.getPolicyByCode(PolicyConst.DEFAULT_CODE, PolicyConst.DEFAULT_LANG);
        return ResponseEntity.ok(policyService.listVersions(pol.id()));
    }

    /** Bản hiệu lực tại ngày at */
    @GetMapping("/seller-tos/effective")
    public ResponseEntity<VersionResp> getSellerTosEffective(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate at
    ) {
        PolicyResp pol = policyService.getPolicyByCode(PolicyConst.DEFAULT_CODE, PolicyConst.DEFAULT_LANG);
        return ResponseEntity.ok(policyService.getEffectiveAt(pol.id(), at));
    }

    /** Lấy cụ thể 1 version */
    @GetMapping("/seller-tos/versions/{version}")
    public ResponseEntity<VersionResp> getSellerTosVersion(@PathVariable String version) {
        PolicyResp pol = policyService.getPolicyByCode(PolicyConst.DEFAULT_CODE, PolicyConst.DEFAULT_LANG);
        return ResponseEntity.ok(policyService.getVersion(pol.id(), version));
    }

    /** Huỷ bản DRAFT (tuỳ chọn) */
    @DeleteMapping("/seller-tos/draft")
    public ResponseEntity<Void> discardSellerTosDraft() {
        PolicyResp pol = policyService.getPolicyByCode(PolicyConst.DEFAULT_CODE, PolicyConst.DEFAULT_LANG);
        policyService.discardDraft(pol.id());
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/seller-tos/publish-now")
    public ResponseEntity<VersionResp> publishNowSellerTos(
            @Valid @RequestBody PublishNowReq req,
            @RequestHeader(value = "X-User-Id", required = false) String uid
    ) throws Exception {
        return ResponseEntity.ok(policyService.publishNowForSellerTos(req, uid));
    }

}
