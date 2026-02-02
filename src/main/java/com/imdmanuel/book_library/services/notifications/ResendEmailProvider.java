package com.imdmanuel.book_library.services.notifications;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResendEmailProvider implements EmailProvider {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${resend.api.key:}")
    private String apiKey;

    @Value("${notification.email.from:onboarding@resend.dev}")
    private String fromEmail;

    @Override
    public void sendEmail(String to, String subject, String body) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Resend API key is not configured");
        }

        log.info("Attempting to send email via Resend API to {}", to);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey != null ? apiKey : "");

        Map<String, Object> request = new HashMap<>();
        request.put("from", fromEmail);
        request.put("to", new String[] { to });
        request.put("subject", subject);
        request.put("html", body);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        restTemplate.postForObject("https://api.resend.com/emails", entity, String.class);
        log.info("Email sent successfully via Resend to {}", to);
    }

    @Override
    public String getProviderName() {
        return "RESEND";
    }
}
