package com.imdmanuel.book_library.services;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imdmanuel.book_library.enums.NotificationType;
import com.imdmanuel.book_library.models.Loan;
import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.repository.LoanRepository;
import com.imdmanuel.book_library.repository.UserRepository;
import com.imdmanuel.book_library.services.notifications.NotificationService;

import lombok.NonNull;

@Service
public class PenaltyService {
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // Configuration constants
    private static final int STRIKES_PER_OVERDUE = 1;
    private static final int MAX_STRIKES_BEFORE_SUSPENSION = 3;
    private static final int SUSPENSION_DAYS = 7; // 7 days suspension

    public PenaltyService(LoanRepository loanRepository, UserRepository userRepository,
            NotificationService notificationService) {
        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Check for overdue loans and apply penalties
     */
    @Transactional
    public void checkAndApplyPenalties(@NonNull User user) {
        Date now = new Date();
        List<Loan> overdueLoans = loanRepository.findByUserAndReturnDateIsNullAndDueDateBefore(user, now);

        for (Loan loan : overdueLoans) {
            int daysOverdue = calculateDaysOverdue(loan.getDueDate(), now);

            user.setStrikes(user.getStrikes() + daysOverdue * STRIKES_PER_OVERDUE);

        }

        if (user.getStrikes() >= MAX_STRIKES_BEFORE_SUSPENSION) {
            suspendUser(user, SUSPENSION_DAYS);
            notificationService.sendPenaltyNotification(user,
                    NotificationType.SUSPENSION_NOTICE,
                    "Your account has been suspended for " + SUSPENSION_DAYS + " days due to multiple overdue books.");
        } else {
            notificationService.sendPenaltyNotification(user,
                    NotificationType.PENALTY_APPLIED,
                    "You have received " + overdueLoans.size() + " strikes for overdue books.");
        }

        userRepository.save(user);
    }

    /**
     * Apply penalty when returning a late book
     */
    @Transactional
    public void applyReturnPenalty(Loan loan) {
        if (loan.getReturnDate() == null || loan.getDueDate() == null) {
            return;
        }

        if (loan.getReturnDate().after(loan.getDueDate())) {
            User user = loan.getUser();
            int daysOverdue = calculateDaysOverdue(loan.getDueDate(), loan.getReturnDate());

            // Add strike
            user.setStrikes(user.getStrikes() + STRIKES_PER_OVERDUE * daysOverdue);

            if (user.getStrikes() >= MAX_STRIKES_BEFORE_SUSPENSION) {
                suspendUser(user, SUSPENSION_DAYS);
                notificationService.sendPenaltyNotification(user,
                        NotificationType.SUSPENSION_NOTICE,
                        "Your account has been suspended for " + SUSPENSION_DAYS + " days.");
            } else {
                notificationService.sendPenaltyNotification(user,
                        NotificationType.PENALTY_APPLIED,
                        "You have received a strike for late return of " + loan.getBook().getTitle());
            }

            userRepository.save(user);
        }
    }

    private int calculateDaysOverdue(Date dueDate, Date returnDate) {
        if (returnDate == null || dueDate == null) {
            return 0;
        }
        long millisOverdue = returnDate.getTime() - dueDate.getTime();
        if (millisOverdue <= 0) {
            return 0;
        }
        return (int) Math.ceil(millisOverdue / (1000.0 * 60 * 60 * 24));
    }

    private void suspendUser(User user, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, days);
        user.setSuspensionUntil(calendar.getTime());
        user.setSuspended(true);
    }

    /**
     * Check if suspension period has expired
     */
    @Transactional
    public void checkSuspensionStatus(User user) {
        if (user.isSuspended() && user.getSuspensionUntil() != null) {
            if (new Date().after(user.getSuspensionUntil())) {
                user.setSuspended(false);
                user.setSuspensionUntil(null);
                userRepository.save(user);
            }
        }
    }
}
