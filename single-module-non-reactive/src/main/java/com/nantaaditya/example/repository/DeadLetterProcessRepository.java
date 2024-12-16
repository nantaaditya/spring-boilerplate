package com.nantaaditya.example.repository;

import com.nantaaditya.example.entity.DeadLetterProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeadLetterProcessRepository extends JpaRepository<DeadLetterProcess, Long> {

}
