package com.nantaaditya.example.service.impl;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import com.nantaaditya.example.repository.DeadLetterProcessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeadLetterProcessServiceImplTest {

  @InjectMocks
  private DeadLetterProcessServiceImpl deadLetterProcessService;

  @Mock
  private DeadLetterProcessRepository deadLetterProcessRepository;

  @Test
  void remove() {
    doNothing().when(deadLetterProcessRepository).deleteByCreatedDateLessThan(anyLong());

    deadLetterProcessService.remove(30);

    verify(deadLetterProcessRepository).deleteByCreatedDateLessThan(anyLong());
  }
}