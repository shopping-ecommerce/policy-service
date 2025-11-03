package iuh.fit.event.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PolicyEvent {
    LocalDate startDate;      // ngày bắt đầu hiệu lực
    String pdfUrl;
    List<String> emails;
}
