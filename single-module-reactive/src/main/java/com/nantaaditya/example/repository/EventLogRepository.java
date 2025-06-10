package com.nantaaditya.example.repository;

import com.nantaaditya.example.entity.EventLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventLogRepository extends ReactiveCrudRepository<EventLog, String> {

}
