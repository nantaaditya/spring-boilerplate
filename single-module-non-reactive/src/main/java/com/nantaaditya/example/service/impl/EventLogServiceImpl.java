package com.nantaaditya.example.service.impl;

import com.nantaaditya.example.repository.EventLogRepository;
import com.nantaaditya.example.service.internal.EventLogService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventLogServiceImpl implements EventLogService {

  private final EventLogRepository eventLogRepository;

  @Async("defaultAsyncTaskExecutor")
  @Override
  public void remove(int days) {
    LocalDateTime now = LocalDateTime.now();
    eventLogRepository.deleteByCreatedDateBefore(now.minusDays(days));
  }
}
