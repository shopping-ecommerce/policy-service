// src/main/java/iuh/fit/se/service/ConsentService.java
package iuh.fit.se.service;

import iuh.fit.se.dto.response.ConsentStatusBySellerResp;
import iuh.fit.se.dto.response.PolicyConsentResp;

public interface ConsentService {
    // Seller Center: kiểm tra để quyết định hiện modal
    ConsentStatusBySellerResp getSellerTosStatusBySeller(String sellerId);

    // Seller bấm "Đồng ý" -> tạo consent (idempotent)
    PolicyConsentResp acceptSellerTos(String sellerId, String ip, String userAgent);

    // Seller bấm "Từ chối" -> thu hồi quyền, cho 14 ngày trước khi khóa
    void declineSellerTos(String sellerId, String ip, String userAgent);
}
