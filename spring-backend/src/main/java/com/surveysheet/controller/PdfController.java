package com.surveysheet.controller;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.surveysheet.dto.response.PdfStatusResponse;
import com.surveysheet.service.LambdaPdfService;
import com.surveysheet.service.SkillSheetService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
@Slf4j
public class PdfController {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final LambdaPdfService lambdaPdfService;
  private final SkillSheetService skillSheetService;

  @Value("${aws.s3.pdf-bucket-name}")
  private String bucketName;

  private static final Duration PRESIGN_DURATION = Duration.ofMinutes(10);

  /** ポーリング用：S3にPDFがあればpresigned URLを返し、無ければ生成中として返す。 */
  @GetMapping("/{id}/status")
  public ResponseEntity<PdfStatusResponse> status(@PathVariable UUID id) {
    String key = buildKey(id);

    if (objectExists(key)) {
      return ResponseEntity.ok(PdfStatusResponse.ready(presign(key)));
    }
    return ResponseEntity.ok(PdfStatusResponse.generating());
  }

  /** 手動リトライ用：既存シートに対して再度Lambdaを非同期Invokeする。 */
  @PostMapping("/{id}/regenerate")
  public ResponseEntity<PdfStatusResponse> regenerate(@PathVariable UUID id) {
    // 未存在・期限切れの場合はfindById()と同じ例外になり、GlobalExceptionHandlerが404・410に変換する
    String userName = skillSheetService.findUserNameById(id);
    // スロットリングで Invoke しなかった場合も 202 を返す。直近の Invoke による生成が進んでいるため、
    // フロントはそのままポーリングを続ければよい
    lambdaPdfService.regenerate(id, userName);
    return ResponseEntity.accepted().body(PdfStatusResponse.generating());
  }

  // Lambda（aws-lambda/src/index.ts）が PDF を保存するキーと同じ形式にする
  private String buildKey(UUID id) {
    return "skill-sheets/" + id + ".pdf";
  }

  private boolean objectExists(String key) {
    try {
      s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    } catch (S3Exception e) {
      log.error("S3のHeadObjectでエラーが発生しました: key={}, statusCode={}", key, e.statusCode(), e);
      throw e;
    }
  }

  private String presign(String key) {
    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder().signatureDuration(PRESIGN_DURATION)
        .getObjectRequest(GetObjectRequest.builder().bucket(bucketName).key(key).build()).build();
    return s3Presigner.presignGetObject(presignRequest).url().toString();
  }
}
