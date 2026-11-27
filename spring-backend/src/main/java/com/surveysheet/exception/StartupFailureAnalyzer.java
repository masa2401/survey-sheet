package com.surveysheet.exception;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** 管理者が直す間違いなので、スタックトレースではなく、間違いの一覧と直し方を出す（META-INF/spring.factories で登録） */
public class StartupFailureAnalyzer extends AbstractFailureAnalyzer<StartupException> {

  @Override
  protected FailureAnalysis analyze(Throwable rootFailure, StartupException cause) {
    return new FailureAnalysis(cause.getMessage(), "設定や定義ファイルを直してから、起動し直してください。", cause);
  }
}
