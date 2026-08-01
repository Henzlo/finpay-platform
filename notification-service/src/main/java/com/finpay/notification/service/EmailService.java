package com.finpay.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    /**
     * @return true when SMTP accepted the message; false on failure (never throws)
     */
    public boolean sendEmail(String to, String subject, String body) {
        try {
            if (to == null || to.isBlank()) {
                log.warn("Skipping email: empty recipient subject={}", subject);
                return false;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to={} subject={}", to, subject);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send email to={} subject={}", to, subject, ex);
            return false;
        }
    }

    public boolean sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            if (to == null || to.isBlank()) {
                log.warn("Skipping HTML email: empty recipient subject={}", subject);
                return false;
            }
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(mimeMessage);
            log.info("HTML email sent to={} subject={}", to, subject);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send HTML email to={} subject={}", to, subject, ex);
            return false;
        }
    }
}
