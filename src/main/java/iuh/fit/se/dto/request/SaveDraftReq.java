package iuh.fit.se.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class SaveDraftReq {
    @NotBlank
    private String code;
    @NotBlank
    private String title;
    private String version;
    private String contentMd;
    private String changeNotes;
    private BigDecimal commissionPercent;
    private Boolean requireReconsent;

    public SaveDraftReq() {}
    public SaveDraftReq(String code, String langIgnored, String version, String contentMd,
                        String changeNotes, BigDecimal commissionPercent, Boolean requireReconsent) {
        this.code = code;
        this.title = "Điều khoản Người bán"; // cho tiện
        this.version = version;
        this.contentMd = contentMd;
        this.changeNotes = changeNotes;
        this.commissionPercent = commissionPercent;
        this.requireReconsent = requireReconsent;
    }
    // getters/setters
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public String getVersion() { return version; }
    public String getContentMd() { return contentMd; }
    public String getChangeNotes() { return changeNotes; }
    public java.math.BigDecimal getCommissionPercent() { return commissionPercent; }
    public Boolean getRequireReconsent() { return requireReconsent; }
}
