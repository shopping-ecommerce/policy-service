// src/main/java/iuh/fit/se/config/PolicyAutoRevokeProperties.java
package iuh.fit.se.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@ConfigurationProperties(prefix = "policy.auto-revoke")
public class PolicyAutoRevokeProperties {
    /** Số ngày chờ phản hồi kể từ startDate (mặc định 30) */
    private int days = 30;
    /** Cron chạy job (mặc định 02:10 hằng ngày) */
    private String cron = "0 10 2 * * *";
    /** Bật/tắt job */
    private boolean enabled = true;
}
