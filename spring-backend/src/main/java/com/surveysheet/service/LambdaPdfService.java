package com.surveysheet.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lambda（skillsheet-pdf-generator）への非同期Invokeを担当するサービス。
 * InvocationType.EVENT を使うため、呼び出し自体の成否のみを検証し、
 * PDF生成の成否は GET /api/pdf/{id}/status 側でS3の存在確認により判断する。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LambdaPdfService {

  private final LambdaClient lambdaClient;
  private final JsonMapper jsonMapper;

  @Value("${aws.lambda.pdf-function-name}")
  private String functionName;

  @Value("${app.frontend-base-url}")
  private String frontendBaseUrl;

  // 再生成（regenerate）の連打防止用の簡易スロットリング。保存時の Invoke も記録するため、間隔は直近の Invoke から数える。
  // 判定に使うのは直近 MIN_INVOKE_INTERVAL_SECONDS 以内の記録だけなので、それより古い記録は定期的に削除する
  private final ConcurrentHashMap<UUID, Instant> lastInvokedAt = new ConcurrentHashMap<>();
  private static final long MIN_INVOKE_INTERVAL_SECONDS = 15;

  /** 「再試行」ボタンから呼ばれる。直近のInvokeから間隔が短すぎる場合はスキップする。 */
  public void regenerate(UUID sheetId, String userName) {
    Instant now = Instant.now();
    Instant last = lastInvokedAt.get(sheetId);
    if (last != null && now.minusSeconds(MIN_INVOKE_INTERVAL_SECONDS).isBefore(last)) {
      log.info("PDF再生成リクエストをスロットリングしました: sheetId={}", sheetId);
      return;
    }
    requestGenerationAsync(sheetId, userName);
  }

  /**
   * シート保存のコミット後に PDF 生成を依頼する。
   * コミット前に Invoke すると、Lambda が未コミットのシートを取得しにいく可能性があるため。
   * トランザクションの外で発行されたイベントは捨てられるので、発行側（SkillSheetService.save）は @Transactional が前提
   */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onSheetSaved(SheetSavedEvent event) {
    requestGenerationAsync(event.sheetId(), event.userName());
  }

  /** 非同期生成リクエスト。失敗してもログのみで例外を上位に投げない。 */
  public void requestGenerationAsync(UUID sheetId, String userName) {
    try {
      // 共有リンク（ID方式）の形式。フロントエンドの shareUtils.ts（createShareUrlById）と合わせる
      String resultUrl = frontendBaseUrl + "/#/result?id=" + sheetId;
      PdfGenerationPayload payload = new PdfGenerationPayload(
          sheetId.toString(), resultUrl, userName);
      String json = jsonMapper.writeValueAsString(payload);

      InvokeRequest request = InvokeRequest.builder().functionName(functionName)
          .invocationType(InvocationType.EVENT).payload(SdkBytes.fromString(json, StandardCharsets.UTF_8))
          .build();

      InvokeResponse response = lambdaClient.invoke(request);
      lastInvokedAt.put(sheetId, Instant.now());

      if (response.statusCode() != 202) {
        log.warn("Lambda Invokeが想定外のステータスを返しました: sheetId={}, status={}",
            sheetId, response.statusCode());
      } else {
        log.info("Lambda PDF生成をInvokeしました: sheetId={}, status={}", sheetId, response.statusCode());
      }
    } catch (LambdaException e) {
      log.error("Lambda PDF生成のInvokeに失敗しました: sheetId={}", sheetId, e);
    } catch (Exception e) {
      log.error("PDF生成リクエストの送信中に予期しないエラーが発生しました: sheetId={}", sheetId, e);
    }
  }

  /** スロットリングの判定に使われなくなった記録を削除する。放置すると、シートを作るたびに記録が増え続ける。 */
  @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
  public void removeExpiredInvocationRecords() {
    removeExpiredInvocationRecordsAt(Instant.now());
  }

  // 基準時刻を受け取る版。テストから任意の時刻で呼べるようにしている
  void removeExpiredInvocationRecordsAt(Instant now) {
    Instant threshold = now.minusSeconds(MIN_INVOKE_INTERVAL_SECONDS);
    // ConcurrentHashMap の removeIf は、判定後に値が更新されていれば削除しない
    lastInvokedAt.values().removeIf(last -> last.isBefore(threshold));
  }

  private record PdfGenerationPayload(String id, String url, String fileName) {
  }
}
