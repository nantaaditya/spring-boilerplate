package com.nantaaditya.example.model.dto;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CachedBodyInputStream extends ServletInputStream {
  private InputStream cachedBodyInputStream;

  public CachedBodyInputStream(byte[] cachedBody) {
    this.cachedBodyInputStream = new ByteArrayInputStream(cachedBody);
  }

  @Override
  public boolean isFinished() {
    try {
      return cachedBodyInputStream.available() == 0;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void setReadListener(ReadListener readListener) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int read() throws IOException {
    return cachedBodyInputStream.read();
  }
}
