package iuh.fit.se.repository.httpclient;

import iuh.fit.se.config.AuthenticationRequestInterceptor;
import iuh.fit.se.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "user-service",configuration = {AuthenticationRequestInterceptor.class}
)
public interface UserClient {
    @GetMapping("/sellers/emails")
    public ApiResponse<List<String>> getAllSellerEmails();
    @DeleteMapping("/sellers/deleteSeller")
    public ApiResponse<Void> deleteSeller(@RequestParam("sellerId") String sellerId, @RequestParam("reason") String reason);
}
