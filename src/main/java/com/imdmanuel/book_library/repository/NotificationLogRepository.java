package com.imdmanuel.book_library.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.imdmanuel.book_library.models.NotificationLog;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
}
