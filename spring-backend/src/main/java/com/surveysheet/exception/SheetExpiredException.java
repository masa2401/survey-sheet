package com.surveysheet.exception;

public class SheetExpiredException extends RuntimeException {
  private final long expiryDays;

  public SheetExpiredException(String message, long expiryDays) {
    super(message);
    this.expiryDays = expiryDays;
  }

  public long getExpiryDays() {
    return expiryDays;
  }
}
