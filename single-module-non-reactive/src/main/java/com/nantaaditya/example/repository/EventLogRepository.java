package com.nantaaditya.example.repository;

import com.nantaaditya.example.entity.EventLog;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, String> {
  @Transactional
  void deleteByCreatedDateBefore(LocalDateTime date);
}
