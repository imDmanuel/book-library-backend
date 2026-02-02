package com.imdmanuel.book_library.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.imdmanuel.book_library.models.NotificationPreference;
import com.imdmanuel.book_library.models.User;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    Optional<NotificationPreference> findByUser(User user);
}
