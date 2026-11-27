package com.surveysheet.exception;

/** 同一クライアントからのリクエストが短時間に集中した場合にスローされる例外。 */
public class TooManyRequestsException extends RuntimeException {
  private final long retryAfterSeconds;

  public TooManyRequestsException(String message, long retryAfterSeconds) {
    super(message);
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}
