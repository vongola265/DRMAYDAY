package com.cnw.medical_system.service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {
    // Lưu danh sách token và thời gian hết hạn trong Ram
    private final Map<String, Date> blacklist = new ConcurrentHashMap<>();

    // Đưa token và thời gian hết hạn vào danh sách
    public void blacklistToken(String token, Date expiryTime) {
        blacklist.put(token, expiryTime);
    }

    // kiểm tra xem token có nằm trong danh sách đen hay không
    public boolean isBlacklisted(String token) {
        Date expiryTime = blacklist.get(token);
        if (expiryTime == null)
            return false;
        // nếu token trong blacklist đã quá hạn -> tự động dọn dẹp khỏi ram
        if (expiryTime.before(new Date())) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }
}
