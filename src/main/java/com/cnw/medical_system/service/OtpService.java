package com.cnw.medical_system.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class OtpService {
    private static class OtpData {
        String code;
        LocalDateTime expiryTime;

        OtpData(String code, LocalDateTime expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
        }
    }

    // Key là soDienThoai, Value là thoogn tin mã otp
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();

    // Sinh mã OTP trong 6 chữ số , lưu RAM sống trong 5 phút
    public String generateOTP(String soDienThoai) {
        String otpCode = String.valueOf(100000 + new Random().nextInt(900000));
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);
        otpStorage.put(soDienThoai, new OtpData(otpCode, expiryTime));
        return otpCode;
    }

    // xác thực mã OTP; nếu đúng và còn hạn thì xóa mã và trả về true
    public boolean validateOtp(String soDienThoai, String maOtp) {
        OtpData otpData = otpStorage.get(soDienThoai);
        if (otpData == null) {
            return false;
        }
        // Kiển tra quá hạn 5p chưa
        if (LocalDateTime.now().isAfter(otpData.expiryTime)) {
            otpStorage.remove(soDienThoai);
            return false;
        }
        // so khớp mã OTP
        if (otpData.code.equals(maOtp)) {
            otpStorage.remove(soDienThoai);// dùng xong xóa ngay
            return true;
        }
        return false;
    }
}
