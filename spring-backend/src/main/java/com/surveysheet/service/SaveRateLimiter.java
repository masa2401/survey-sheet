package com.surveysheet.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.surveysheet.exception.TooManyRequestsException;

/**
 * スキルシート新規作成（POST /api/sheets）に対する簡易スロットリング。
 *
 * インメモリのスライディングウィンドウ方式。単一インスタンス運用（現状のRailwayデプロイ）を
 * 前提としており、複数インスタンス構成に拡張する場合はRedis等の共有ストアへの置き換えが必要。
 * （PdfController/LambdaPdfServiceの再試行スロットリングと同じ設計方針に揃えている）
 */
@Component
public class SaveRateLimiter {

  @Value("${sheet.save.rate-limit.max-requests:20}")
  private int maxRequests;

  @Value("${sheet.save.rate-limit.window-seconds:600}")
  private long windowSeconds;

  private final ConcurrentHashMap<String, Deque<Instant>> history = new ConcurrentHashMap<>();

  /**
   * 直近のウィンドウ内でのリクエスト数が上限を超えていれば{@link TooManyRequestsException}を投げる。
   * 上限内であれば今回のリクエストを記録して正常終了する。
   */
  public void checkAndRecord(String clientKey) {
    Instant now = Instant.now();
    Duration window = Duration.ofSeconds(windowSeconds);

    history.compute(clientKey, (key, existing) -> {
      Deque<Instant> timestamps = existing != null ? existing : new ArrayDeque<>();

      removeOutsideWindow(timestamps, now, window);

      if (timestamps.size() >= maxRequests) {
        long retryAfterSeconds = window.minus(Duration.between(timestamps.peekFirst(), now)).getSeconds();
        throw new TooManyRequestsException(
            "スキルシートの作成リクエストが上限に達しました。しばらく時間をおいて再度お試しください",
            Math.max(retryAfterSeconds, 1));
      }

      timestamps.addLast(now);
      return timestamps;
    });
  }

  /** ウィンドウを過ぎた記録を削除し、記録が空になったクライアントはキーごと取り除く。 */
  @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
  public void removeExpiredRecords() {
    removeExpiredRecordsAt(Instant.now());
  }

  // 基準時刻を受け取る版。テストから任意の時刻で呼べるようにしている
  void removeExpiredRecordsAt(Instant now) {
    Duration window = Duration.ofSeconds(windowSeconds);
    // computeIfPresent はキー単位で排他されるため、同じクライアントの checkAndRecord と競合しない。
    // null を返すとそのキーが削除される
    history.keySet().forEach(clientKey -> history.computeIfPresent(clientKey, (key, timestamps) -> {
      removeOutsideWindow(timestamps, now, window);
      return timestamps.isEmpty() ? null : timestamps;
    }));
  }

  private static void removeOutsideWindow(Deque<Instant> timestamps, Instant now, Duration window) {
    while (!timestamps.isEmpty() && Duration.between(timestamps.peekFirst(), now).compareTo(window) > 0) {
      timestamps.pollFirst();
    }
  }
}
