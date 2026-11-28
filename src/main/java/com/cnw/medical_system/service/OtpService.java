package com.cnw.medical_system.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.cnw.medical_system.exception.ApiContractException;

@Service
public class OtpService {
    private static class OtpData {
        String code;
        LocalDateTime expiryTime;
        int failedAttempts;

        OtpData(String code, LocalDateTime expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
            this.failedAttempts = 0;
        }
    }

    // Key là soDienThoai, Value là thông tin mã otp
    private final Map<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, Integer> requestCount = new ConcurrentHashMap<>();

    // Sinh mã OTP trong 6 chữ số , lưu RAM sống trong 5 phút
    public String generateOTP(String soDienThoai) {
        int count = requestCount.getOrDefault(soDienThoai, 0);
        if (count >= 5) {
            throw new ApiContractException(HttpStatus.FORBIDDEN, "1009", "Bạn đã yêu cầu OTP quá số lần cho phép");
        }
        requestCount.put(soDienThoai, count + 1);

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
        // Kiểm tra quá hạn 5p chưa
        if (LocalDateTime.now().isAfter(otpData.expiryTime)) {
            otpStorage.remove(soDienThoai);
            return false;
        }
        // so khớp mã OTP
        if (otpData.code.equals(maOtp)) {
            otpStorage.remove(soDienThoai);// dùng xong xóa ngay
            requestCount.remove(soDienThoai);
            return true;
        }
        otpData.failedAttempts++;
        if (otpData.failedAttempts >= 5) {
            otpStorage.remove(soDienThoai);
        }
        return false;
    }
}
