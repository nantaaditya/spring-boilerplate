package com.nantaaditya.example.repository;

import com.nantaaditya.example.entity.DeadLetterProcess;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeadLetterProcessRepository extends JpaRepository<DeadLetterProcess, Long> {

  @Transactional
  void deleteByCreatedDateLessThan(long createdDate);

  Page<DeadLetterProcess> findByProcessTypeAndProcessNameAndProcessed(String processType,
      String ProcessName, boolean processed, Pageable pageable);
}
