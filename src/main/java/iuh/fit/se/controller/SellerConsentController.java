// src/main/java/iuh/fit/se/controller/SellerConsentController.java
package iuh.fit.se.controller;

import iuh.fit.se.dto.response.ConsentStatusBySellerResp;
import iuh.fit.se.dto.response.PolicyConsentResp;
import iuh.fit.se.service.ConsentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/policies/seller-tos")
public class SellerConsentController {

    ConsentService consentService;

    /** FE gọi để biết có cần bật modal ở Seller Center không */
    @GetMapping("/status/sellers/{sellerId}")
    public ResponseEntity<ConsentStatusBySellerResp> getStatus(@PathVariable String sellerId) {
        return ResponseEntity.ok(consentService.getSellerTosStatusBySeller(sellerId));
    }

    /** Seller bấm "Đồng ý" -> tạo consent (idempotent), tự lấy version hiện hành */
    @PostMapping("/consents/sellers/{sellerId}/accept")
    public ResponseEntity<PolicyConsentResp> accept(
            @PathVariable String sellerId,
            HttpServletRequest request
    ) {
        String ip = extractClientIp(request);
        String ua = request.getHeader("User-Agent");
        return ResponseEntity.ok(consentService.acceptSellerTos(sellerId, ip, ua));
    }

    /** Seller bấm "Từ chối" -> thu hồi quyền + thông báo còn 14 ngày */
    @PostMapping("/consents/sellers/{sellerId}/decline")
    public ResponseEntity<Void> decline(@PathVariable String sellerId, HttpServletRequest request) {
        String ip = extractClientIp(request);
        String ua = request.getHeader("User-Agent");
        consentService.declineSellerTos(sellerId,ip, ua);
        return ResponseEntity.noContent().build();
    }

    private String extractClientIp(HttpServletRequest req) {
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) return h.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
