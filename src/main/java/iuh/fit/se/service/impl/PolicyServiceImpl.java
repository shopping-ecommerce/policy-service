package iuh.fit.se.service.impl;

import iuh.fit.event.dto.PolicyEvent;
import iuh.fit.se.constants.PolicyConst;
import iuh.fit.se.dto.request.CreatePolicyWithVersionReq;
import iuh.fit.se.dto.request.PublishNowReq;
import iuh.fit.se.dto.request.SaveDraftReq;
import iuh.fit.se.dto.response.CreatePolicyWithVersionResp;
import iuh.fit.se.dto.response.PolicyResp;
import iuh.fit.se.dto.response.VersionResp;
import iuh.fit.se.entity.Policy;
import iuh.fit.se.entity.PolicyVersion;
import iuh.fit.se.repository.PolicyRepository;
import iuh.fit.se.repository.PolicyVersionRepository;
import iuh.fit.se.repository.httpclient.FileClient;
import iuh.fit.se.repository.httpclient.UserClient;
import iuh.fit.se.service.PolicyService;
import iuh.fit.se.utils.DocUtils;
import iuh.fit.se.utils.InMemoryMultipartFile;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PolicyServiceImpl implements PolicyService {

    PolicyRepository policyRepository;
    PolicyVersionRepository policyVersionRepository;
    FileClient fileClient;
    UserClient userClient;
    KafkaTemplate<String, Object> kafkaTemplate;

    // src/main/java/iuh/fit/se/service/impl/PolicyServiceImpl.java
    @Override
    @Transactional
    public VersionResp getOrCreateDraftByCode(String code, String lang, String defaultTitle, String createdBy) {
        final String usedLang = (lang == null || lang.isBlank()) ? "vi" : lang;

        // 1) Tìm hoặc tạo Policy
        Policy policy = policyRepository.findByCodeAndLang(code, usedLang)
                .orElseGet(() -> policyRepository.save(
                        Policy.builder().code(code).title(defaultTitle).lang(usedLang).build()
                ));

        // 2) Nếu đã có DRAFT → trả về luôn
        var drafts = policyVersionRepository.findDrafts(policy.getId());
        if (!drafts.isEmpty()) {
            return map(drafts.get(0));
        }

        // 3) Không có DRAFT → tìm nguồn để "fill" nội dung cho nháp mới
        // 3.1. Ưu tiên version đang hiệu lực gần nhất tại hôm nay
        var today = java.time.LocalDate.now();
        PolicyVersion source = policyVersionRepository.findEffectiveAt(policy.getId(), today).orElse(null);

        // 3.2. Nếu chưa có effective tại hôm nay → lấy PUBLISHED mới nhất (start_date desc)
        if (source == null) {
            var pubs = policyVersionRepository.findPublishedDesc(policy.getId());
            if (!pubs.isEmpty()) {
                source = pubs.get(0);
            }
        }

        // 4) Chuẩn bị dữ liệu nháp
        String nextVersion = autoNextVersion(policy.getId());
        java.math.BigDecimal cp = new java.math.BigDecimal("8.00");
        String content = "# Điều khoản\n"; // mặc định rỗng
        Boolean reconsent = Boolean.FALSE;
        String notes = "Khởi tạo nháp";

        if (source != null) {
            // clone một số field từ source để admin chỉnh tiếp
            content = (source.getContentMd() == null || source.getContentMd().isBlank())
                    ? content : source.getContentMd();
            cp = (source.getCommissionPercent() == null) ? cp : source.getCommissionPercent();
            reconsent = (source.getRequireReconsent() == null) ? reconsent : source.getRequireReconsent();
            notes = "Clone từ " + source.getVersion();
        }

        // 5) Tạo DRAFT mới từ nguồn (hoặc rỗng nếu không có nguồn)
        PolicyVersion draft = PolicyVersion.builder()
                .policyId(policy.getId())
                .version(nextVersion)
                .status("DRAFT")
                .contentMd(content)
                .commissionPercent(cp)
                .requireReconsent(reconsent)
                .changeNotes(notes)
                .createdBy(createdBy)
                .build();
        draft = policyVersionRepository.save(draft);

        // Trỏ currentVersionId vào draft để UI mở lại là có dữ liệu
        policy.setCurrentVersionId(draft.getId());
        policy.setUpdatedAt(java.time.LocalDateTime.now());
        policyRepository.save(policy);

        return map(draft);
    }

    /* ======================= Helper mapping ======================= */
    private VersionResp map(PolicyVersion v) {
        return new VersionResp(
                v.getId(),
                v.getVersion(),
                v.getStatus(),
                v.getStartDate(),
                v.getEndDate(),
                v.getContentMd(),
                v.getPdfUrl(),
                v.getPdfSha256(),
                v.getCommissionPercent(),
                v.getChangeNotes()
        );
    }

    /* ======================= Lưu/Tạo DRAFT duy nhất ======================= */
    @Transactional
    @Override
    public VersionResp saveDraft(SaveDraftReq req, String createdBy) {
        final String code = req.getCode().trim();
        final String title = req.getTitle().trim();
        final String lang = "vi";

        // Policy: find or create, update title nếu đổi
        Policy policy = policyRepository.findByCodeAndLang(code, lang)
                .map(p -> {
                    if (!Objects.equals(p.getTitle(), title)) {
                        p.setTitle(title);
                        p.setUpdatedAt(LocalDateTime.now());
                        return policyRepository.save(p);
                    }
                    return p;
                })
                .orElseGet(() -> policyRepository.save(
                        Policy.builder().code(code).title(title).lang(lang).build()
                ));

        // Commission validate
        BigDecimal cp = req.getCommissionPercent() == null ? new BigDecimal("8.00") : req.getCommissionPercent();
        if (cp.compareTo(new BigDecimal("0")) < 0 || cp.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("commissionPercent must be in [0,100]");
        }

        // 1 draft duy nhất
        var drafts = policyVersionRepository.findDrafts(policy.getId());
        PolicyVersion draft = drafts.isEmpty() ? null : drafts.get(0);

        if (draft != null) {
            draft.setContentMd(req.getContentMd());
            draft.setCommissionPercent(cp);
            draft.setChangeNotes(req.getChangeNotes());
            draft.setRequireReconsent(Boolean.TRUE.equals(req.getRequireReconsent()));
            draft.setCreatedBy(draft.getCreatedBy() == null ? createdBy : draft.getCreatedBy());
            draft.setUpdatedAt(LocalDateTime.now());

            // cho phép đổi version nếu không trùng
            if (req.getVersion() != null && !req.getVersion().isBlank()) {
                String wanted = normalizeVersion(req.getVersion());
                policyVersionRepository.findByPolicyIdAndVersion(policy.getId(), wanted)
                        .ifPresent(v -> { throw new IllegalArgumentException("Version đã tồn tại: " + wanted); });
                draft.setVersion(wanted);
            }

            draft = policyVersionRepository.save(draft);
        } else {
            String version = (req.getVersion() == null || req.getVersion().isBlank())
                    ? autoNextVersion(policy.getId())
                    : normalizeVersion(req.getVersion());

            policyVersionRepository.findByPolicyIdAndVersion(policy.getId(), version)
                    .ifPresent(v -> { throw new IllegalArgumentException("Version đã tồn tại: " + version); });

            draft = PolicyVersion.builder()
                    .policyId(policy.getId())
                    .version(version)
                    .status("DRAFT")
                    .contentMd(req.getContentMd())
                    .commissionPercent(cp)
                    .changeNotes(req.getChangeNotes())
                    .requireReconsent(Boolean.TRUE.equals(req.getRequireReconsent()))
                    .createdBy(createdBy)
                    .build();
            draft = policyVersionRepository.save(draft);

            // trỏ currentVersionId vào draft
            policy.setCurrentVersionId(draft.getId());
            policy.setUpdatedAt(LocalDateTime.now());
            policyRepository.save(policy);
        }

        return map(draft);
    }


    @Override
    public PolicyResp getPolicyByCode(String code, String lang) {
        Policy p = policyRepository.findByCodeAndLang(code, (lang == null || lang.isBlank()) ? "vi" : lang)
                .orElseThrow(() -> new IllegalArgumentException("Policy không tồn tại với code=" + code));
        return new PolicyResp(p.getId(), p.getCode(), p.getTitle(), p.getLang(), p.getCurrentVersionId());
    }

    @Override
    public List<VersionResp> listVersions(String policyId) {
        policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy không tồn tại: " + policyId));
        return policyVersionRepository.findByPolicyIdOrderByCreatedAtDesc(policyId)
                .stream().map(this::map).toList();
    }

        public VersionResp getDraft(String policyId) {
        policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy không tồn tại: " + policyId));
        var drafts = policyVersionRepository.findDrafts(policyId);
        if (drafts.isEmpty()) throw new IllegalArgumentException("Không có bản DRAFT.");
        return map(drafts.get(0));
    }

    @Override
    public VersionResp getEffectiveAt(String policyId, LocalDate at) {
        policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy không tồn tại: " + policyId));
        var v = policyVersionRepository.findEffectiveAt(policyId, at)
                .orElseThrow(() -> new IllegalArgumentException("Không có bản hiệu lực tại " + at));
        return map(v);
    }

    @Override
    public VersionResp getVersion(String policyId, String version) {
        policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy không tồn tại: " + policyId));
        var v = policyVersionRepository.findByPolicyIdAndVersion(policyId, normalizeVersion(version))
                .orElseThrow(() -> new IllegalArgumentException("Version không tồn tại: " + version));
        return map(v);
    }

    @Transactional
    @Override
    public void discardDraft(String policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy không tồn tại: " + policyId));
        var drafts = policyVersionRepository.findDrafts(policyId);
        if (drafts.isEmpty()) return;

        PolicyVersion draft = drafts.get(0);
        policyVersionRepository.deleteById(draft.getId());

        // Nếu currentVersionId đang trỏ vào DRAFT → chuyển sang bản PUBLISHED mới nhất (nếu có)
        if (draft.getId().equals(policy.getCurrentVersionId())) {
            var pubs = policyVersionRepository.findPublishedDesc(policyId);
            if (!pubs.isEmpty()) {
                policy.setCurrentVersionId(pubs.get(0).getId());
            } else {
                policy.setCurrentVersionId(null);
            }
            policy.setUpdatedAt(LocalDateTime.now());
            policyRepository.save(policy);
        }
    }

    /* ======================= Core publish (private) ======================= */
    /** Publish bản DRAFT: tạo PDF, upload, “đóng” bản đang hiệu lực nếu chồng lấp, cập nhật currentVersionId */
    private void publishDraft(Policy policy, PolicyVersion draft, LocalDate startDate) throws Exception {
        if (!"DRAFT".equalsIgnoreCase(draft.getStatus())) {
            throw new IllegalStateException("Chỉ publish được bản DRAFT.");
        }

        // Nếu có bản đang effective tại startDate → set endDate = startDate.minusDays(1)
        policyVersionRepository.findEffectiveAt(policy.getId(), startDate)
                .ifPresent(v -> {
                    v.setEndDate(startDate.minusDays(1));
                    v.setUpdatedAt(LocalDateTime.now());
                    policyVersionRepository.save(v);
                });

        // Render XHTML → PDF và upload
        var xhtml = DocUtils.mdToXhtml(draft.getContentMd(), policy.getTitle(), draft.getVersion(), startDate.toString());
        var pdfBytes = DocUtils.htmlToPdfBytes(xhtml);
        var sha = DocUtils.sha256Hex(pdfBytes);

        var fileName = "policy_%s_%s.pdf".formatted(policy.getId(), draft.getVersion());
        var mf = new InMemoryMultipartFile("file", fileName, "application/pdf", pdfBytes);

        var upload = fileClient.uploadPdf(mf);
        if (upload == null || upload.getResult() == null) {
            throw new RuntimeException("Failed to upload PDF to file service");
        }

        // Cập nhật DRAFT -> PUBLISHED
        draft.setStatus("PUBLISHED");
        draft.setStartDate(startDate);
        draft.setPdfUrl(upload.getResult());
        draft.setPdfSha256(sha);
        draft.setPublishedAt(LocalDateTime.now());
        draft.setUpdatedAt(LocalDateTime.now());
        policyVersionRepository.save(draft);

        // currentVersionId → bản vừa publish
        policy.setCurrentVersionId(draft.getId());
        policy.setUpdatedAt(LocalDateTime.now());
        policyRepository.save(policy);
    }

    /* ======================= Version helpers ======================= */
    /** Tính version tiếp theo dạng vX.Y (nếu chưa có gì → v1.0; ngược lại tăng minor) */
    private String autoNextVersion(String policyId) {
        var versions = policyVersionRepository.findAllByPolicyIdOrderByCreatedAtDesc(policyId);
        if (versions.isEmpty()) return "v1.0";
        String latest = versions.get(0).getVersion();
        String[] p = parseVersion(latest);
        int major = Integer.parseInt(p[0]);
        int minor = Integer.parseInt(p[1]);
        minor += 1;
        return "v" + major + "." + minor;
    }

    private String normalizeVersion(String v) {
        v = v.trim().toLowerCase();
        if (!v.startsWith("v")) v = "v" + v;
        String[] p = parseVersion(v);
        return "v" + Integer.parseInt(p[0]) + "." + Integer.parseInt(p[1]);
    }

    private String[] parseVersion(String v) {
        String s = v.trim().toLowerCase();
        if (s.startsWith("v")) s = s.substring(1);
        String major = "1", minor = "0";
        if (s.contains(".")) {
            String[] arr = s.split("\\.", 2);
            major = safeInt(arr[0], "1");
            minor = safeInt(arr[1], "0");
        } else {
            major = safeInt(s, "1");
        }
        return new String[]{major, minor};
    }

    private String safeInt(String s, String def) {
        try { return String.valueOf(Integer.parseInt(s)); }
        catch (Exception e) { return def; }
    }
    @Transactional
    @Override
    public VersionResp publishNowForSellerTos(PublishNowReq req, String createdBy) throws Exception {
        final String code = PolicyConst.DEFAULT_CODE; // "SELLER_TOS"
        final String lang = PolicyConst.DEFAULT_LANG; // "vi"
        final String defaultTitle = PolicyConst.DEFAULT_TITLE;

        // 1) Lấy/hoặc tạo DRAFT (nếu chưa có, clone từ bản hiệu lực gần nhất hoặc rỗng)
        VersionResp vr = getOrCreateDraftByCode(code, lang, defaultTitle, createdBy);

        // 2) Lấy thực thể để override
        Policy policy = policyRepository.findByCodeAndLang(code, lang)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy policy sau khi khởi tạo."));
        PolicyVersion draft = policyVersionRepository.findById(vr.id())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy draft vừa khởi tạo."));

        // 3) Override các field nếu client gửi (cho phép 1-click Đăng)
        if (req.version() != null && !req.version().isBlank()) {
            String wanted = normalizeVersion(req.version());
            policyVersionRepository.findByPolicyIdAndVersion(policy.getId(), wanted)
                    .ifPresent(v -> { throw new IllegalArgumentException("Version đã tồn tại: " + wanted); });
            draft.setVersion(wanted);
        }
        if (req.contentMd() != null) {
            draft.setContentMd(req.contentMd());
        }
        if (req.changeNotes() != null) {
            draft.setChangeNotes(req.changeNotes());
        }
        if (req.commissionPercent() != null) {
            var cp = req.commissionPercent();
            if (cp.compareTo(new BigDecimal("0")) < 0 || cp.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("commissionPercent phải trong [0,100].");
            }
            draft.setCommissionPercent(cp);
        }
        if (req.requireReconsent() != null) {
            draft.setRequireReconsent(req.requireReconsent());
        }
        if (draft.getCreatedBy() == null) {
            draft.setCreatedBy(createdBy);
        }
        draft.setUpdatedAt(LocalDateTime.now());
        draft = policyVersionRepository.save(draft);

        // 4) Validate theo ngữ cảnh publish
        validateDraftForPublish(policy, draft, req.startDate());

        // 5) Publish (đóng bản cũ nếu chồng lấp, render PDF, upload, set PUBLISHED)
        publishDraft(policy, draft, req.startDate());
        var x = userClient.getAllSellerEmails();
        log.info("Đã lấy {} email người bán từ user-service sau khi publish Seller TOS.", (x == null ? 0 : x.getResult()));
        kafkaTemplate.send("policy-notification", PolicyEvent.builder()
                .startDate(req.startDate())
                .pdfUrl(draft.getPdfUrl()) // Pass the correct pdfUrl value
                .emails(x == null ? List.of() : x.getResult()) // Ensure emails are not null
                .build()
        );
        // 6) Trả về
        return map(draft);
    }
    private void validateDraftForPublish(Policy policy, PolicyVersion draft, LocalDate startDate) {
        if (startDate == null) throw new IllegalArgumentException("startDate là bắt buộc khi publish.");

        if (draft.getVersion() == null || draft.getVersion().isBlank())
            throw new IllegalArgumentException("Version của bản nháp đang trống.");

        String v = draft.getVersion().trim().toLowerCase();
        if (!v.matches("^v?\\d+(\\.\\d+)?$"))
            throw new IllegalArgumentException("Version không hợp lệ. Gợi ý: v1.0, v2.3, ...");

        String md = draft.getContentMd();
        if (md == null || md.isBlank())
            throw new IllegalArgumentException("Nội dung đang trống, không thể publish.");
        if (md.strip().length() < 30)
            throw new IllegalArgumentException("Nội dung quá ngắn (<30 ký tự).");

        if (policy.getTitle() == null || policy.getTitle().isBlank())
            throw new IllegalArgumentException("Tiêu đề policy đang trống.");

        if (draft.getCommissionPercent() == null)
            throw new IllegalArgumentException("commissionPercent bị thiếu.");
        var cp = draft.getCommissionPercent();
        if (cp.compareTo(new BigDecimal("0")) < 0 || cp.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("commissionPercent phải trong [0,100].");
    }

}
