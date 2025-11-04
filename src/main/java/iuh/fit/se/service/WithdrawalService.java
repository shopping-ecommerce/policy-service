package iuh.fit.se.service;

public interface WithdrawalService {
    void withdrawSellerTos(String sellerId, String reason, String ip, String userAgent);
}
