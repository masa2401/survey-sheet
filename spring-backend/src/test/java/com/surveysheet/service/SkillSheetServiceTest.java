package com.surveysheet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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

@ExtendWith(MockitoExtension.class)
class SkillSheetServiceTest {

  @Mock
  private SkillSheetRepository sheetRepository;

  @Mock
  private SheetCategoryRepository categoryRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private SkillSheetService service;

  @Test
  @DisplayName("save: リクエストが正常にエンティティに変換され、保存されてIDが返ること")
  void save_Success() {
    // GIVEN: 階層構造を持ったテストデータを作成
    AnswerDto answerDto = new AnswerDto(1, 3);
    QuestionDto questionDto = new QuestionDto(1, List.of(answerDto));
    CategoryDto categoryDto = new CategoryDto(2, List.of(questionDto));
    SaveSheetRequest request = new SaveSheetRequest("山田太郎", List.of(categoryDto));

    // 保存後にリポジトリが返すダミーのエンティティ（ID付き）を用意
    UUID expectedId = UUID.randomUUID();
    SkillSheet savedSheet = new SkillSheet();
    savedSheet.setId(expectedId);

    when(sheetRepository.save(ArgumentMatchers.<SkillSheet>any())).thenReturn(savedSheet);

    // WHEN: テスト対象メソッドの実行
    UUID actualId = service.save(request);
    // THEN: アサーション（AssertJのassertThatを使用）
    assertThat(actualId).isEqualTo(expectedId);

    // リポジトリのsaveメソッドが本当に呼ばれたかどうかの検証
    verify(sheetRepository).save(ArgumentMatchers.<SkillSheet>any());

    // PDF生成のきっかけとなる保存イベントが発行されることを検証（Invoke はコミット後にリスナーが行う）
    verify(eventPublisher).publishEvent(new SheetSavedEvent(expectedId, "山田太郎"));
  }

  @Test
  @DisplayName("findById: カテゴリと回答をまとめて取得し、設問ごとにまとめて返すこと")
  void findById_Success() {
    // GIVEN: 有効期限内のシートと、回答を2件持つカテゴリ
    UUID targetId = UUID.randomUUID();
    SkillSheet sheet = new SkillSheet();
    sheet.setUserName("山田太郎");
    sheet.setExpiresAt(LocalDateTime.now().plusDays(1));

    SheetCategory category = new SheetCategory();
    category.setCategoryId(2);
    category.getAnswers().add(answer(1, 10, 3));
    category.getAnswers().add(answer(1, 11, 4));

    when(sheetRepository.findById(targetId)).thenReturn(Optional.of(sheet));
    when(categoryRepository.findWithAnswersBySheetId(targetId)).thenReturn(List.of(category));

    // WHEN
    SheetResponse response = service.findById(targetId);

    // THEN
    assertThat(response.userName()).isEqualTo("山田太郎");
    assertThat(response.categories()).containsExactly(
        new CategoryDto(2, List.of(new QuestionDto(1, List.of(new AnswerDto(10, 3), new AnswerDto(11, 4))))));
  }

  @Test
  @DisplayName("findById: 該当するスキルシートが存在しない場合、NoSuchElementExceptionがスローされること")
  void findById_NotFound_ThrowsException() {
    // GIVEN: リポジトリが空（Optional.empty()）を返すように設定
    UUID targetId = UUID.randomUUID();
    when(sheetRepository.findById(targetId)).thenReturn(Optional.empty());

    // WHEN & THEN: 例外が発生することを検証
    assertThatThrownBy(() -> service.findById(targetId)).isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("スキルシートが見つかりません");
  }

  @Test
  @DisplayName("findById: expiresAt が過去の場合、SheetExpiredException がスローされること")
  void findById_Expired_ThrowsException() {
    UUID targetId = UUID.randomUUID();
    SkillSheet expiredSheet = new SkillSheet();
    expiredSheet.setExpiresAt(LocalDateTime.now().minusDays(1)); // 1日前に期限切れ

    when(sheetRepository.findById(targetId)).thenReturn(Optional.of(expiredSheet));

    assertThatThrownBy(() -> service.findById(targetId))
        .isInstanceOf(SheetExpiredException.class);
  }

  @Test
  @DisplayName("findUserNameById: 有効期限内のシートの名前が返ること")
  void findUserNameById_Success() {
    UUID targetId = UUID.randomUUID();
    SkillSheet sheet = new SkillSheet();
    sheet.setUserName("山田太郎");
    sheet.setExpiresAt(LocalDateTime.now().plusDays(1));

    when(sheetRepository.findById(targetId)).thenReturn(Optional.of(sheet));

    assertThat(service.findUserNameById(targetId)).isEqualTo("山田太郎");
  }

  @Test
  @DisplayName("findUserNameById: 該当するスキルシートが存在しない場合、NoSuchElementExceptionがスローされること")
  void findUserNameById_NotFound_ThrowsException() {
    UUID targetId = UUID.randomUUID();
    when(sheetRepository.findById(targetId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findUserNameById(targetId))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  @DisplayName("findUserNameById: expiresAt が過去の場合、SheetExpiredException がスローされること")
  void findUserNameById_Expired_ThrowsException() {
    UUID targetId = UUID.randomUUID();
    SkillSheet expiredSheet = new SkillSheet();
    expiredSheet.setExpiresAt(LocalDateTime.now().minusDays(1));

    when(sheetRepository.findById(targetId)).thenReturn(Optional.of(expiredSheet));

    assertThatThrownBy(() -> service.findUserNameById(targetId))
        .isInstanceOf(SheetExpiredException.class);
  }

  @Test
  @DisplayName("deleteExpiredSheets: 現在時刻より前に期限が切れたシートの削除を依頼すること")
  void deleteExpiredSheets_DeletesBeforeNow() {
    LocalDateTime before = LocalDateTime.now();
    when(sheetRepository.deleteExpiredBefore(ArgumentMatchers.any())).thenReturn(1);

    service.deleteExpiredSheets();

    ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(sheetRepository).deleteExpiredBefore(captor.capture());
    assertThat(captor.getValue()).isBetween(before, LocalDateTime.now());
  }

  @Test
  @DisplayName("save: カテゴリがskillSheetのコレクションに正しく追加されること")
  void save_CategoriesAreAttachedToSheet() {
    AnswerDto answerDto = new AnswerDto(1, 3);
    QuestionDto questionDto = new QuestionDto(1, List.of(answerDto));
    CategoryDto categoryDto = new CategoryDto(2, List.of(questionDto));
    SaveSheetRequest request = new SaveSheetRequest("山田太郎", List.of(categoryDto));

    UUID expectedId = UUID.randomUUID();
    SkillSheet savedSheet = new SkillSheet();
    savedSheet.setId(expectedId);
    when(sheetRepository.save(ArgumentMatchers.<SkillSheet>any())).thenReturn(savedSheet);

    service.save(request);

    ArgumentCaptor<SkillSheet> captor = ArgumentCaptor.forClass(SkillSheet.class);
    verify(sheetRepository).save(captor.capture());
    assertThat(captor.getValue().getCategories()).hasSize(1);
    assertThat(captor.getValue().getCategories().get(0).getAnswers()).hasSize(1);
  }

  private SheetAnswer answer(int questionId, int answerId, int value) {
    SheetAnswer answer = new SheetAnswer();
    answer.setQuestionId(questionId);
    answer.setAnswerId(answerId);
    answer.setValue(value);
    return answer;
  }
}
