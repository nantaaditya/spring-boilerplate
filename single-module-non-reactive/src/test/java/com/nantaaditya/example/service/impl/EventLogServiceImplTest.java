package com.nantaaditya.example.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import com.nantaaditya.example.repository.EventLogRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventLogServiceImplTest {

  @InjectMocks
  private EventLogServiceImpl eventLogService;

  @Mock
  private EventLogRepository eventLogRepository;

  @Test
  void remove() {
    doNothing().when(eventLogRepository)
        .deleteByCreatedDateBefore(any(LocalDateTime.class));
    eventLogService.remove(30);
    verify(eventLogRepository)
        .deleteByCreatedDateBefore(any(LocalDateTime.class));
  }
}