package com.nantaaditya.example.model.dto;

public interface DeadLetterCapable {

  String getProcessType();

  String getProcessName();

  byte[] getPayload();
}
