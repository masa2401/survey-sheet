package com.surveysheet.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.surveysheet.dto.AnswerDto;
import com.surveysheet.dto.CategoryDto;
import com.surveysheet.dto.QuestionDto;
import com.surveysheet.dto.request.SaveSheetRequest;
import com.surveysheet.dto.response.SheetResponse;
import com.surveysheet.entity.SheetAnswer;
import com.surveysheet.entity.SheetCategory;
import com.surveysheet.entity.SkillSheet;
import com.surveysheet.exception.SheetExpiredException;
import com.surveysheet.repository.SheetCategoryRepository;
import com.surveysheet.repository.SkillSheetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SkillSheetService {

  private final SkillSheetRepository sheetRepository;
  private final SheetCategoryRepository categoryRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Value("${sheet.expiry.days:5}")
  private long expiryDays;

  public UUID save(SaveSheetRequest req) {
    SkillSheet sheet = new SkillSheet();
    sheet.setUserName(req.userName());
    sheet.setShareToken(UUID.randomUUID().toString());
    sheet.setExpiresAt(LocalDateTime.now().plusDays(expiryDays));

    for (CategoryDto catDto : req.categories()) {
      SheetCategory category = new SheetCategory();
      category.setCategoryId(catDto.categoryId());
      category.setSkillSheet(sheet);

      for (QuestionDto qDto : catDto.questions()) {
        for (AnswerDto aDto : qDto.answers()) {
          SheetAnswer answer = new SheetAnswer();
          answer.setQuestionId(qDto.questionId());
          answer.setAnswerId(aDto.answerId());
          answer.setValue(aDto.value());
          answer.setSheetCategory(category);
          category.getAnswers().add(answer);
        }
      }

      sheet.getCategories().add(category);
    }

    UUID savedId = sheetRepository.save(sheet).getId();

    eventPublisher.publishEvent(new SheetSavedEvent(savedId, req.userName()));

    return savedId;
  }

  @Scheduled(cron = "${sheet.cleanup.cron:0 0 3 * * *}")
  public void deleteExpiredSheets() {
    int deletedCount = sheetRepository.deleteExpiredBefore(LocalDateTime.now());
    if (deletedCount > 0) {
      log.info("期限切れスキルシートを{}件削除しました", deletedCount);
    }
  }

  @Transactional(readOnly = true)
  public SheetResponse findById(UUID id) {
    SkillSheet sheet = findActiveSheet(id);

    List<CategoryDto> categories = categoryRepository.findWithAnswersBySheetId(id).stream()
        .map(cat -> new CategoryDto(cat.getCategoryId(), buildQuestions(cat.getAnswers()))).toList();

    return new SheetResponse(sheet.getUserName(), categories);
  }

  /**
   * IDでシートの名前だけを取得する（PDF再生成用）。
   * 回答（categories）は遅延ロードのため読み込まれず、findById より軽い。
   */
  @Transactional(readOnly = true)
  public String findUserNameById(UUID id) {
    return findActiveSheet(id).getUserName();
  }

  // 未存在・期限切れのシートは例外にする（GlobalExceptionHandlerが404・410に変換する）
  private SkillSheet findActiveSheet(UUID id) {
    SkillSheet sheet = sheetRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("スキルシートが見つかりません"));

    if (sheet.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new SheetExpiredException("この共有リンクの有効期限は切れています", expiryDays);
    }
    return sheet;
  }

  private List<QuestionDto> buildQuestions(List<SheetAnswer> answers) {
    return answers.stream().collect(Collectors.groupingBy(a -> a.getQuestionId())).entrySet().stream()
        .map(entry -> {
          List<AnswerDto> answerDtos = entry.getValue().stream()
              .map(a -> new AnswerDto(a.getAnswerId(), a.getValue())).toList();
          return new QuestionDto(entry.getKey(), answerDtos);
        }).toList();
  }
}
