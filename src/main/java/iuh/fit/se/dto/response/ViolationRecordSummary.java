package iuh.fit.se.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationRecordSummary {
    private String id;
    private String violationType;
    private String description;
    private List<String> evidenceUrls;
    private LocalDateTime reportedAt;
    private Integer violationNumber;
}

