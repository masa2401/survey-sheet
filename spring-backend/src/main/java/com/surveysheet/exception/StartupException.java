package com.surveysheet.exception;

/** 設定や定義ファイルの間違いで、起動を止めるときに投げる。文は StartupFailureAnalyzer が表示する */
public class StartupException extends RuntimeException {

  public StartupException(String message) {
    super(message);
  }
}
