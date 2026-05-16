package com.mfa.project.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.mfa.project.entity.Employee;
import com.mfa.project.entity.QrToken;
import com.mfa.project.repository.EmployeeRepository;
import com.mfa.project.repository.QrTokenRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class QrTokenService {

    private final QrTokenRepository qrTokenRepository;
    private final EmployeeRepository employeeRepository;

    public QrTokenService(QrTokenRepository qrTokenRepository,
                          EmployeeRepository employeeRepository) {
        this.qrTokenRepository = qrTokenRepository;
        this.employeeRepository = employeeRepository;
    }

    public QrToken generateTokenForEmployee(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        QrToken qrToken = new QrToken(token, expiresAt, employee);

        return qrTokenRepository.save(qrToken);
    }

    public String generateQrBase64(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            var bitMatrix = qrCodeWriter.encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    300,
                    300
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Cannot generate QR code", e);
        }
    }
}