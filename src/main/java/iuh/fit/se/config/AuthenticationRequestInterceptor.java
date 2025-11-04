// src/main/java/iuh/fit/se/config/AuthenticationRequestInterceptor.java
package iuh.fit.se.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticationRequestInterceptor implements RequestInterceptor {

    private final ServiceAuthProps authProps; // cấu hình bên dưới

    @Override
    public void apply(RequestTemplate template) {
        var attrs = RequestContextHolder.getRequestAttributes();

        if (attrs instanceof ServletRequestAttributes sra) {
            var req = sra.getRequest();
            var auth = req.getHeader("Authorization");
            if (StringUtils.hasText(auth)) {
                template.header("Authorization", auth);
            }
            var userId = req.getHeader("X-User-Id");
            if (StringUtils.hasText(userId)) {
                template.header("X-User-Id", userId);
            }
        } else {
            // Không có request (batch / job / async) → dùng thông tin service-to-service
            if (StringUtils.hasText(authProps.getServiceToken())) {
                template.header("Authorization", "Bearer " + authProps.getServiceToken());
            }
            template.header("X-Caller", "policy-service");
        }
    }
}
