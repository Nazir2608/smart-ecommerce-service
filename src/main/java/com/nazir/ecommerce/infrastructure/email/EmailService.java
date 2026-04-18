package com.nazir.ecommerce.infrastructure.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@smartcommerce.dev}")
    private String fromAddress;

    @Value("${app.name:SmartCommerce Pro}")
    private String appName;

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        String subject = appName + " — Password Reset OTP";
        String body = """
                <html><body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2 style="color:#1a1a2e">Password Reset Request</h2>
                  <p>You requested a password reset for your %s account.</p>
                  <p>Use the following OTP to reset your password. It expires in <strong>10 minutes</strong>.</p>
                  <div style="background:#f4f4f4;border-radius:8px;padding:24px;text-align:center;margin:24px 0">
                    <span style="font-size:36px;font-weight:bold;letter-spacing:8px;color:#1a1a2e">%s</span>
                  </div>
                  <p style="color:#666">If you did not request this, you can safely ignore this email.</p>
                  <hr style="border:none;border-top:1px solid #eee"/>
                  <p style="color:#999;font-size:12px">%s — do not reply to this email.</p>
                </body></html>
                """.formatted(appName, otp, appName);
        sendHtmlEmail(toEmail, subject, body);
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        String subject = "Welcome to " + appName + "!";
        String body = """
                <html><body style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2 style="color:#1a1a2e">Welcome, %s!</h2>
                  <p>Your account on <strong>%s</strong> has been created successfully.</p>
                  <p>You can now browse thousands of products, track your orders, and enjoy personalised
                     AI-powered recommendations.</p>
                  <a href="#" style="background:#1a1a2e;color:#fff;padding:12px 24px;border-radius:6px;
                     text-decoration:none;display:inline-block;margin-top:16px">Start Shopping</a>
                  <hr style="border:none;border-top:1px solid #eee;margin-top:32px"/>
                  <p style="color:#999;font-size:12px">%s</p>
                </body></html>
                """.formatted(fullName, appName, appName);
        sendHtmlEmail(toEmail, subject, body);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, appName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} — subject: {}", to, subject);
        } catch (MessagingException | java.io.UnsupportedEncodingException ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
        }
    }
}
