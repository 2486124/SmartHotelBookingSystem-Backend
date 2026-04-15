package com.cts.project.shbs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Your Password Reset Request — Smart Hotel Booking System");
        message.setText(
            "Dear Valued User,\n\n" +
            "We received a request to reset the password for your Smart Hotel Booking System (SHBS) account.\n" +
            "Please use the secure link below to create a new password.\n\n" +
            "-----------------------------------------------\n" +
            "Reset My Password: " + resetLink + "\n" +
            "-----------------------------------------------\n\n" +
            "Important: This link will expire in 15 minutes for your security.\n" +
            "If you did not initiate this request, please disregard this email — your account has not been compromised.\n\n" +
            "Should you need further assistance, our support team is always happy to help.\n\n" +
            "Best regards,\n" +
            "Customer Support Team\n" +
            "Smart Hotel Booking System"
        );

        mailSender.send(message);
    }
}