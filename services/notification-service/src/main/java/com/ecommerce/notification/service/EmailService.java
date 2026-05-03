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
 * JavaMailSender → Spring'in e-posta sınıfı.
 * yml'de SMTP ayarları yapılır (bu projede gmail kullandım).
 *
 * Thymeleaf'i e-posta şablonları için kullandım.
 *
 * sendSimple → Düz metin e-posta (hızlı)
 * sendHtml   → HTML e-posta (thymeleaf şablonlu)
 * 
 * setFrom kısmında noreply@ecommerce.com kullandım. Fakat smtp configlerinde
 * kendi mail adresim olduğu için gönderen olarak kendi mail adresim set ediliyor.
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
