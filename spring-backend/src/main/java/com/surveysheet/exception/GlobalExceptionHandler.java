package com.surveysheet.exception;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(NoSuchElementException e) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(body);
  }

  @ExceptionHandler(SheetExpiredException.class)
  public ResponseEntity<ProblemDetail> handleExpired(SheetExpiredException e) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, e.getMessage());
    body.setProperty("expiryDays", e.getExpiryDays());
    return ResponseEntity
        .status(HttpStatus.GONE)
        .body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException e) {
    String message = e.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .collect(Collectors.joining(", "));
    ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(body);
  }

  // パスのIDがUUIDの形式でない場合（/api/sheets/abc など）は、該当するリソースが無いものとして404を返す
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    boolean isPathVariable = e.getParameter().hasParameterAnnotation(PathVariable.class);
    HttpStatus status = isPathVariable ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
    String detail = isPathVariable ? "指定されたリソースが見つかりません" : "リクエストのパラメータが不正です";
    return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ProblemDetail> handleNotReadable(HttpMessageNotReadableException e) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "リクエストの形式が不正です");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(S3Exception.class)
  public ResponseEntity<ProblemDetail> handleS3Error(S3Exception e) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
        "PDFステータスの確認中にエラーが発生しました");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  @ExceptionHandler(TooManyRequestsException.class)
  public ResponseEntity<ProblemDetail> handleTooManyRequests(TooManyRequestsException e) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    body.setProperty("retryAfterSeconds", e.getRetryAfterSeconds());
    return ResponseEntity
        .status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", String.valueOf(e.getRetryAfterSeconds()))
        .body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(Exception e) {
    // 存在しないパス（404）や未対応のメソッド（405）など、Spring MVC が定義済みのエラーは
    // 例外自身が持つステータスで返す。ここで500にすると、クライアント側の誤りがサーバーエラーとして記録される
    if (e instanceof ErrorResponse errorResponse) {
      return ResponseEntity.status(errorResponse.getStatusCode())
          .headers(errorResponse.getHeaders())
          .body(errorResponse.getBody());
    }
    log.error("予期しないエラーが発生しました", e);
    ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "サーバー内部でエラーが発生しました");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}
