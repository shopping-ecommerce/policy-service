package iuh.fit.se.dto.request;

import java.math.BigDecimal;

public class SaveDraftBody {
    private String version;                 // optional
    private String contentMd;               // optional
    private String changeNotes;             // optional
    private BigDecimal commissionPercent;   // optional
    private Boolean requireReconsent;       // optional

    public String getVersion() { return version; }
    public String getContentMd() { return contentMd; }
    public String getChangeNotes() { return changeNotes; }
    public BigDecimal getCommissionPercent() { return commissionPercent; }
    public Boolean getRequireReconsent() { return requireReconsent; }
}
