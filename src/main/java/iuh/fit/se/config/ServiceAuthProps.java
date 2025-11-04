package iuh.fit.se.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter @Setter
@Component
@ConfigurationProperties(prefix = "policy.service-auth")
public class ServiceAuthProps {
    /** JWT theo client-credentials (nếu bạn phát hành sẵn) */
    private String serviceToken;
}