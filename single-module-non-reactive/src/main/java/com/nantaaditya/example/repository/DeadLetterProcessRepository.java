package com.nantaaditya.example.repository;

import com.nantaaditya.example.entity.DeadLetterProcess;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeadLetterProcessRepository extends JpaRepository<DeadLetterProcess, Long> {
  @Transactional
  void deleteByCreatedDateBeforeAndStatus(LocalDateTime dateTime, String status);

  Page<DeadLetterProcess> findByProcessTypeAndProcessNameAndStatusIn(String processType,
      String processName, Set<String> statuses, Pageable pageable);
}
