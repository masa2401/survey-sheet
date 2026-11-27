package com.surveysheet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.surveysheet.exception.TooManyRequestsException;

class SaveRateLimiterTest {

  private SaveRateLimiter limiter;

  @BeforeEach
  void setUp() {
    limiter = new SaveRateLimiter();
    ReflectionTestUtils.setField(limiter, "maxRequests", 2);
    ReflectionTestUtils.setField(limiter, "windowSeconds", 1L);
  }

  @Test
  @DisplayName("上限回数以内であれば例外は発生しない")
  void withinLimit_doesNotThrow() {
    assertThatCode(() -> {
      limiter.checkAndRecord("1.2.3.4");
      limiter.checkAndRecord("1.2.3.4");
    }).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("上限回数を超えるとTooManyRequestsExceptionがスローされる")
  void exceedsLimit_throwsTooManyRequests() {
    limiter.checkAndRecord("1.2.3.4");
    limiter.checkAndRecord("1.2.3.4");

    assertThatThrownBy(() -> limiter.checkAndRecord("1.2.3.4"))
        .isInstanceOf(TooManyRequestsException.class);
  }

  @Test
  @DisplayName("クライアント（IP）が異なれば互いにカウントは影響しない")
  void differentClients_areCountedIndependently() {
    limiter.checkAndRecord("1.1.1.1");
    limiter.checkAndRecord("1.1.1.1");

    assertThatCode(() -> limiter.checkAndRecord("2.2.2.2")).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("ウィンドウ経過後は再度リクエストできる")
  void afterWindowElapses_allowsRequestsAgain() throws InterruptedException {
    limiter.checkAndRecord("1.2.3.4");
    limiter.checkAndRecord("1.2.3.4");

    Thread.sleep(1100);

    assertThatCode(() -> limiter.checkAndRecord("1.2.3.4")).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("スロットリングされたリクエストはカウントに加算されない")
  void throttledRequest_isNotRecorded() {
    limiter.checkAndRecord("1.2.3.4");
    limiter.checkAndRecord("1.2.3.4");

    assertThatThrownBy(() -> limiter.checkAndRecord("1.2.3.4"))
        .isInstanceOf(TooManyRequestsException.class);
    // 3回目（スロットリングされた分）がカウントされていれば、ここでも例外になるはず
    assertThatThrownBy(() -> limiter.checkAndRecord("1.2.3.4"))
        .isInstanceOf(TooManyRequestsException.class);
  }

  @Test
  @DisplayName("定期削除：ウィンドウを過ぎた記録はクライアントごと削除される")
  void removeExpiredRecords_removesClientsOutsideWindow() {
    limiter.checkAndRecord("1.2.3.4");

    // ウィンドウ（1秒）より後の時刻を基準に削除する
    limiter.removeExpiredRecordsAt(Instant.now().plusSeconds(2));

    assertThat(history()).doesNotContainKey("1.2.3.4");
  }

  @Test
  @DisplayName("定期削除：ウィンドウ内の記録は削除されない")
  void removeExpiredRecords_keepsClientsWithinWindow() {
    limiter.checkAndRecord("1.2.3.4");

    limiter.removeExpiredRecordsAt(Instant.now());

    assertThat(history()).containsKey("1.2.3.4");
  }

  @SuppressWarnings("unchecked")
  private Map<String, ?> history() {
    return (Map<String, ?>) ReflectionTestUtils.getField(limiter, "history");
  }
}
