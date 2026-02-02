package com.imdmanuel.book_library.services.notifications;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.imdmanuel.book_library.enums.NotificationType;
import com.imdmanuel.book_library.models.Loan;
import com.imdmanuel.book_library.repository.LoanRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final LoanRepository loanRepository;
    private final NotificationService notificationService;

    /**
     * Daily job to send due date reminders (at 9:00 AM)
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendDueReminders() {
        log.info("Starting scheduled due date reminders job");

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_YEAR, 3);

        // Logical check: due in exactly 3 days (between start of day and end of day)
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date start = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date end = calendar.getTime();

        List<Loan> dueSoon = loanRepository.findByReturnDateIsNullAndDueDateBetween(start, end);
        log.info("Found {} loans due in 3 days", dueSoon.size());

        for (Loan loan : dueSoon) {
            notificationService.sendLoanNotification(loan, NotificationType.LOAN_DUE_REMINDER);
        }
    }

    /**
     * Daily job to send overdue notices (at 10:00 AM)
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void sendOverdueNotices() {
        log.info("Starting scheduled overdue notices job");

        Date now = new Date();
        List<Loan> overdueLoans = loanRepository.findByReturnDateIsNullAndDueDateBefore(now);
        log.info("Found {} overdue loans", overdueLoans.size());

        for (Loan loan : overdueLoans) {
            notificationService.sendLoanNotification(loan, NotificationType.LOAN_OVERDUE);
        }
    }
}
