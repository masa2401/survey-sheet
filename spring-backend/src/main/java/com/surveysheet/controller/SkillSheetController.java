package com.surveysheet.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.surveysheet.dto.request.SaveSheetRequest;
import com.surveysheet.dto.response.SaveSheetResponse;
import com.surveysheet.dto.response.SheetResponse;
import com.surveysheet.service.SaveRateLimiter;
import com.surveysheet.service.SkillSheetService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sheets")
@RequiredArgsConstructor
public class SkillSheetController {
  private final SkillSheetService skillSheetService;
  private final SaveRateLimiter saveRateLimiter;

  @PostMapping
  public ResponseEntity<SaveSheetResponse> save(@RequestBody @Valid SaveSheetRequest req,
      HttpServletRequest httpRequest) {
    saveRateLimiter.checkAndRecord(resolveClientIp(httpRequest));
    UUID id = skillSheetService.save(req);
    return ResponseEntity.status(HttpStatus.CREATED).body(new SaveSheetResponse(id));
  }

  @GetMapping("/{id}")
  public ResponseEntity<SheetResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(skillSheetService.findById(id));
  }

  // Railway のエッジプロキシ経由では getRemoteAddr() がプロキシの IP を返すため、ヘッダーから取得する。
  // X-Real-IP はプロキシが接続元の IP で上書きするので、クライアントは偽装できない。
  // X-Forwarded-For はクライアントが送った値の末尾にプロキシが接続元を付け足すため、先頭は偽装でき、末尾だけが信頼できる
  private String resolveClientIp(HttpServletRequest request) {
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
    }
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      String[] addresses = forwardedFor.split(",");
      return addresses[addresses.length - 1].trim();
    }
    return request.getRemoteAddr();
  }
}
