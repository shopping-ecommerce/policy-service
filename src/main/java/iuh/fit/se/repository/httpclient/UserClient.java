package iuh.fit.se.repository.httpclient;

import iuh.fit.se.config.AuthenticationRequestInterceptor;
import iuh.fit.se.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "user-service",configuration = {AuthenticationRequestInterceptor.class}
)
public interface UserClient {
    @GetMapping("/sellers/emails")
    public ApiResponse<List<String>> getAllSellerEmails();
}
