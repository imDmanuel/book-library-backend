package com.imdmanuel.book_library.services.notifications;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.imdmanuel.book_library.enums.NotificationStatus;
import com.imdmanuel.book_library.enums.NotificationType;
import com.imdmanuel.book_library.models.Loan;
import com.imdmanuel.book_library.models.NotificationLog;
import com.imdmanuel.book_library.models.NotificationPreference;
import com.imdmanuel.book_library.models.Reservation;
import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.repository.NotificationLogRepository;
import com.imdmanuel.book_library.repository.NotificationPreferenceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final List<EmailProvider> emailProviders;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationLogRepository logRepository;
    private final TemplateEngine templateEngine;

    @Async
    @Transactional
    public void sendLoanNotification(Loan loan, NotificationType type) {
        User user = loan.getUser();
        NotificationPreference prefs = getOrCreatePreferences(user);

        if (!prefs.isEmailEnabled() || !prefs.isLoanNotifications()) {
            return;
        }

        Map<String, Object> data = Map.of(
                "user", user,
                "loan", loan,
                "book", loan.getBook());

        String template = switch (type) {
            case LOAN_CONFIRMATION -> "email/loan-confirmation";
            case LOAN_RETURN_CONFIRMATION -> "email/return-confirmation";
            case LOAN_DUE_REMINDER -> "email/due-reminder";
            case LOAN_OVERDUE -> "email/overdue-notice";
            case PENALTY_APPLIED -> "email/penalty-notice";
            case SUSPENSION_NOTICE -> "email/suspension-notice";
            case ACCOUNT_CREATED -> "email/account-created";
            default -> throw new IllegalArgumentException("Unsupported loan notification type: " + type);
        };

        String subject = switch (type) {
            case LOAN_CONFIRMATION -> "Loan Confirmation: " + loan.getBook().getTitle();
            case LOAN_RETURN_CONFIRMATION -> "Return Confirmation: " + loan.getBook().getTitle();
            case LOAN_DUE_REMINDER -> "Reminder: Book Due Soon";
            case LOAN_OVERDUE -> "Urgent: Book Overdue";
            case PENALTY_APPLIED -> "Penalty Applied";
            case SUSPENSION_NOTICE -> "Account Suspended";
            case ACCOUNT_CREATED -> "Welcome to Book Library!";
            default -> "Book Library Notification";
        };

        sendEmail(user, type, subject, template, data);
    }

    @Async
    @Transactional
    public void sendReservationNotification(Reservation reservation, NotificationType type) {
        User user = reservation.getUser();
        NotificationPreference prefs = getOrCreatePreferences(user);

        if (!prefs.isEmailEnabled() || !prefs.isReservationNotifications()) {
            return;
        }

        Map<String, Object> data = Map.of(
                "user", user,
                "reservation", reservation,
                "book", reservation.getBook());

        String template = switch (type) {
            case RESERVATION_CONFIRMATION -> "email/reservation-confirmation";
            case RESERVATION_AVAILABLE -> "email/reservation-available";
            case RESERVATION_EXPIRED -> "email/reservation-expired";
            default -> throw new IllegalArgumentException("Unsupported reservation notification type: " + type);
        };

        String subject = switch (type) {
            case RESERVATION_CONFIRMATION -> "Reservation Confirmed: " + reservation.getBook().getTitle();
            case RESERVATION_AVAILABLE -> "Book Available for Pickup: " + reservation.getBook().getTitle();
            case RESERVATION_EXPIRED -> "Reservation Expired: " + reservation.getBook().getTitle();
            default -> "Book Library Notification";
        };

        sendEmail(user, type, subject, template, data);
    }

    @Async
    @Transactional
    public void sendAccountCreatedNotification(User user) {
        Map<String, Object> data = Map.of(
                "user", user);

        sendEmail(user, NotificationType.ACCOUNT_CREATED, "Welcome to Book Library!", "email/account-created", data);
    }

    private void sendEmail(User user, NotificationType type, String subject, String templateName,
            Map<String, Object> data) {
        String htmlBody = null;
        boolean sent = false;
        Exception lastException = null;
        String successfulProvider = null;

        try {
            Context context = new Context();
            context.setVariables(data);
            htmlBody = templateEngine.process(templateName, context);

            // Try providers in order: Resend first (Primary), SMTP second (Fallback)
            for (EmailProvider provider : emailProviders) {
                try {
                    provider.sendEmail(user.getEmail(), subject, htmlBody);
                    sent = true;
                    successfulProvider = provider.getProviderName();
                    break;
                } catch (Exception e) {
                    log.error("Failed to send email via {}: {}", provider.getProviderName(), e.getMessage());
                    lastException = e;
                }
            }
        } catch (Exception e) {
            log.error("Error during email preparation or sending for {}: {}", type, e.getMessage());
            lastException = e;
        }

        NotificationLog logEntry = NotificationLog.builder()
                .user(user)
                .notificationType(type)
                .recipient(user.getEmail())
                .subject(subject)
                .message(htmlBody)
                .provider(sent ? successfulProvider : "NONE")
                .status(sent ? NotificationStatus.SENT : NotificationStatus.FAILED)
                .errorMessage(
                        sent ? null : (lastException != null ? lastException.getMessage() : "All providers failed"))
                .sentAt(sent ? LocalDateTime.now() : null)
                .build();

        logRepository.save(logEntry);
    }

    @Async
    @Transactional
    public void sendPenaltyNotification(User user, NotificationType type, String message) {
        NotificationPreference prefs = getOrCreatePreferences(user);

        if (!prefs.isEmailEnabled() || !prefs.isPenaltyNotifications()) {
            return;
        }

        Map<String, Object> data = Map.of(
                "user", user,
                "message", message);

        String template = switch (type) {
            case PENALTY_APPLIED -> "email/penalty-notice";
            case SUSPENSION_NOTICE -> "email/suspension-notice";
            default -> throw new IllegalArgumentException("Unsupported penalty notification type: " + type);
        };

        String subject = switch (type) {
            case PENALTY_APPLIED -> "Penalty Applied: " + user.getStrikes() + " strikes";
            case SUSPENSION_NOTICE -> "Important: Account Suspended";
            default -> "System Notification";
        };

        sendEmail(user, type, subject, template, data);
    }

    private NotificationPreference getOrCreatePreferences(User user) {
        return preferenceRepository.findByUser(user)
                .orElseGet(() -> {
                    NotificationPreference defaultPrefs = NotificationPreference.builder()
                            .user(user)
                            .build();
                    return preferenceRepository.save(defaultPrefs);
                });
    }
}
