package com.ecommerce.notification.service;

import com.ecommerce.notification.exception.EmailSendException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;

/**
 * E-posta gönderim servisi.
 *
 * JavaMailSender → Spring'in e-posta soyutlaması.
 * yml'de SMTP ayarları yapılır (Gmail, SendGrid vb.).
 * Gerçek e-posta göndermek için SMTP credential gerekir.
 *
 * Geliştirme ortamı için iki seçenek:
 * 1. Mailhog (Docker'da local SMTP yakalayıcı) — gerçek mail gitmez, UI'da görünür
 *    docker run -p 8025:8025 -p 1025:1025 mailhog/mailhog
 * 2. Mailtrap — sandbox SMTP servisi (ücretsiz tier var)
 *
 * Thymeleaf → HTML e-posta şablonları için.
 * Şablon: src/main/resources/templates/email/order-created.html
 *
 * sendSimple → Düz metin e-posta (hızlı)
 * sendHtml   → HTML e-posta (thymeleaf şablonlu)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendSimple(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom("noreply@ecommerce.com");
            mailSender.send(message);
            log.info("E-posta gönderildi: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("E-posta gönderilemedi: to={}, subject={}", to, subject, e);
            throw new EmailSendException("Düz metin e-posta gönderilemedi", e);
        }
    }

    public void sendHtml(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            variables.forEach(context::setVariable);

            String htmlContent = templateEngine.process("email/" + templateName, context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@ecommerce.com");

            mailSender.send(mimeMessage);
            log.info("HTML e-posta gönderildi: to={}, template={}", to, templateName);
        } catch (Exception e) {
            log.error("HTML e-posta gönderilemedi: to={}, template={}", to, templateName, e);
            throw new EmailSendException("HTML e-posta gönderilemedi: " + templateName, e);
        }
    }
}
