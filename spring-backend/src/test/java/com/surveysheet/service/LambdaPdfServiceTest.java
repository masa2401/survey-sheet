package com.surveysheet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LambdaPdfServiceTest {

  private LambdaPdfService service;

  @BeforeEach
  void setUp() {
    // Lambda クライアントと JsonMapper は使わない。Invoke を伴う処理はスパイで差し替える
    service = new LambdaPdfService(null, null);
  }

  @Test
  @DisplayName("保存イベントを受け取ると、同じシートIDと名前でPDF生成を依頼する")
  void onSheetSaved_requestsGeneration() {
    LambdaPdfService spyService = spy(service);
    doNothing().when(spyService).requestGenerationAsync(any(), any());
    UUID sheetId = UUID.randomUUID();

    spyService.onSheetSaved(new SheetSavedEvent(sheetId, "山田太郎"));

    verify(spyService).requestGenerationAsync(sheetId, "山田太郎");
  }

  @Test
  @DisplayName("定期削除：スロットリングの間隔（15秒）を過ぎた記録は削除される")
  void removeExpiredInvocationRecords_removesOldRecords() {
    UUID sheetId = UUID.randomUUID();
    Instant now = Instant.now();
    lastInvokedAt().put(sheetId, now.minusSeconds(16));

    service.removeExpiredInvocationRecordsAt(now);

    assertThat(lastInvokedAt()).doesNotContainKey(sheetId);
  }

  @Test
  @DisplayName("定期削除：スロットリングの間隔内の記録は削除されない")
  void removeExpiredInvocationRecords_keepsRecentRecords() {
    UUID sheetId = UUID.randomUUID();
    Instant now = Instant.now();
    lastInvokedAt().put(sheetId, now.minusSeconds(5));

    service.removeExpiredInvocationRecordsAt(now);

    assertThat(lastInvokedAt()).containsKey(sheetId);
  }

  @SuppressWarnings("unchecked")
  private Map<UUID, Instant> lastInvokedAt() {
    return (Map<UUID, Instant>) ReflectionTestUtils.getField(service, "lastInvokedAt");
  }
}
