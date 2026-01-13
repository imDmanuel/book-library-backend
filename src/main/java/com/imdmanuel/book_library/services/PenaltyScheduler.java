package com.imdmanuel.book_library.services;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.repository.UserRepository;

@Component
public class PenaltyScheduler {

    private final PenaltyService penaltyService;
    private final UserRepository userRepository;

    public PenaltyScheduler(UserRepository userRepository, PenaltyService penaltyService) {
        this.userRepository = userRepository;
        this.penaltyService = penaltyService;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void checkOverDueLoans() {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            penaltyService.checkAndApplyPenalties(user);
            penaltyService.checkSuspensionStatus(user);
        }
    }
}
