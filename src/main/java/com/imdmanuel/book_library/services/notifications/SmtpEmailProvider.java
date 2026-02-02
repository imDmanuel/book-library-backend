package com.imdmanuel.book_library.services.notifications;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String body) throws Exception {
        log.info("Attempting to send email via SMTP to {}", to);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);

        mailSender.send(message);
        log.info("Email sent successfully via SMTP to {}", to);
    }

    @Override
    public String getProviderName() {
        return "SMTP";
    }
}
