package com.imdmanuel.book_library.services.notifications;

public interface EmailProvider {
    void sendEmail(String to, String subject, String body) throws Exception;

    String getProviderName();
}
