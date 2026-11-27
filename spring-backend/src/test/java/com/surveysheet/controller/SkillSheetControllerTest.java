package com.surveysheet.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.surveysheet.dto.AnswerDto;
import com.surveysheet.dto.CategoryDto;
import com.surveysheet.dto.QuestionDto;
import com.surveysheet.dto.request.SaveSheetRequest;
import com.surveysheet.dto.response.SheetResponse;
import com.surveysheet.exception.TooManyRequestsException;
import com.surveysheet.service.SaveRateLimiter;
import com.surveysheet.service.SkillSheetService;

import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(SkillSheetController.class)
class SkillSheetControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JsonMapper objectMapper; // JavaオブジェクトをJSON文字列に変換する用

  @MockitoBean
  private SkillSheetService service;

  @MockitoBean
  private SaveRateLimiter saveRateLimiter;

  @Test
  @DisplayName("POST /api/sheets - スキルシートが正常に保存され、201 CreatedとIDが返ること")
  void save_Success() throws Exception {
    // GIVEN: 擬似的なリクエストデータと、サービスが返す予定のUUIDを用意
    SaveSheetRequest request = new SaveSheetRequest("山田太郎", List.of());
    UUID expectedId = UUID.randomUUID();

    // サービスの挙動をモック（stub）化
    when(service.save(ArgumentMatchers.<SaveSheetRequest>any())).thenReturn(expectedId);

    // WHEN & THEN: リクエストを送信し、結果をアサーション
    mockMvc.perform(post("/api/sheets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(expectedId.toString()));
    // jsonPathを使うと、レスポンスJSONの特定のフィールドを検証できます
  }

  @Test
  @DisplayName("POST /api/sheets - スロットリング上限に達している場合は429 Too Many Requestsが返ること")
  void save_RateLimited_Returns429() throws Exception {
    // GIVEN: レートリミッターが上限超過として例外を投げるよう設定
    SaveSheetRequest request = new SaveSheetRequest("山田太郎", List.of());
    doThrow(new TooManyRequestsException("スキルシートの作成リクエストが上限に達しました。しばらく時間をおいて再度お試しください", 60))
        .when(saveRateLimiter).checkAndRecord(anyString());

    // WHEN & THEN
    mockMvc.perform(post("/api/sheets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "60"))
        .andExpect(jsonPath("$.retryAfterSeconds").value(60));
  }

  @Test
  @DisplayName("POST /api/sheets - X-Real-IPがある場合は、X-Forwarded-Forではなくその値でスロットリングを判定すること")
  void save_UsesRealIpForRateLimit() throws Exception {
    // GIVEN: クライアントが偽のX-Forwarded-Forを付けたリクエスト（X-Real-IPはプロキシが上書きした値）
    SaveSheetRequest request = new SaveSheetRequest("山田太郎", List.of());
    when(service.save(ArgumentMatchers.<SaveSheetRequest>any())).thenReturn(UUID.randomUUID());

    // WHEN
    mockMvc.perform(post("/api/sheets")
        .header("X-Real-IP", "203.0.113.10")
        .header("X-Forwarded-For", "198.51.100.1, 203.0.113.10")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    // THEN
    verify(saveRateLimiter).checkAndRecord("203.0.113.10");
  }

  @Test
  @DisplayName("POST /api/sheets - X-Real-IPが無い場合は、X-Forwarded-Forの末尾（プロキシが付け足した値）で判定すること")
  void save_UsesLastForwardedForWithoutRealIp() throws Exception {
    // GIVEN: 先頭はクライアントが自由に付けられる値、末尾はプロキシが付け足した接続元
    SaveSheetRequest request = new SaveSheetRequest("山田太郎", List.of());
    when(service.save(ArgumentMatchers.<SaveSheetRequest>any())).thenReturn(UUID.randomUUID());

    // WHEN
    mockMvc.perform(post("/api/sheets")
        .header("X-Forwarded-For", "198.51.100.1, 203.0.113.10")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    // THEN
    verify(saveRateLimiter).checkAndRecord("203.0.113.10");
  }

  @Test
  @DisplayName("POST /api/sheets - プロキシのヘッダーが無い場合は、接続元のアドレスで判定すること")
  void save_UsesRemoteAddrWithoutProxyHeaders() throws Exception {
    // GIVEN: ローカル環境のように、プロキシを経由しないリクエスト
    SaveSheetRequest request = new SaveSheetRequest("山田太郎", List.of());
    when(service.save(ArgumentMatchers.<SaveSheetRequest>any())).thenReturn(UUID.randomUUID());

    // WHEN
    mockMvc.perform(post("/api/sheets")
        .with(req -> {
          req.setRemoteAddr("192.0.2.5");
          return req;
        })
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());

    // THEN
    verify(saveRateLimiter).checkAndRecord("192.0.2.5");
  }

  @Test
  @DisplayName("POST /api/sheets - 習熟度が1〜5の範囲外の場合は400 Bad Requestが返ること")
  void save_ValueOutOfRange_Returns400() throws Exception {
    // GIVEN: 習熟度が6の回答を含むリクエスト
    SaveSheetRequest request = new SaveSheetRequest("山田太郎",
        List.of(new CategoryDto(1, List.of(new QuestionDto(1, List.of(new AnswerDto(1, 6)))))));

    // WHEN & THEN
    mockMvc.perform(post("/api/sheets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/sheets - 1設問の回答が上限（30件）を超える場合は400 Bad Requestが返ること")
  void save_TooManyAnswers_Returns400() throws Exception {
    // GIVEN: 31件の回答を持つ設問を含むリクエスト
    List<AnswerDto> answers = IntStream.rangeClosed(1, 31).mapToObj(i -> new AnswerDto(i, 3)).toList();
    SaveSheetRequest request = new SaveSheetRequest("山田太郎",
        List.of(new CategoryDto(1, List.of(new QuestionDto(1, answers)))));

    // WHEN & THEN
    mockMvc.perform(post("/api/sheets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/sheets - カテゴリが上限（10件）を超える場合は400 Bad Requestが返ること")
  void save_TooManyCategories_Returns400() throws Exception {
    // GIVEN: 11件のカテゴリを含むリクエスト
    List<CategoryDto> categories = IntStream.rangeClosed(1, 11).mapToObj(i -> new CategoryDto(i, List.of()))
        .toList();
    SaveSheetRequest request = new SaveSheetRequest("山田太郎", categories);

    // WHEN & THEN
    mockMvc.perform(post("/api/sheets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/sheets - カテゴリIDが無い場合は400 Bad Requestが返ること")
  void save_MissingCategoryId_Returns400() throws Exception {
    // GIVEN: categoryId が null のカテゴリを含むリクエスト
    SaveSheetRequest request = new SaveSheetRequest("山田太郎",
        List.of(new CategoryDto(null, List.of())));

    // WHEN & THEN
    mockMvc.perform(post("/api/sheets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/sheets - 上限内の入れ子データは201 Createdで保存されること")
  void save_NestedWithinLimits_Returns201() throws Exception {
    // GIVEN: 上限ちょうど（30件）の回答を持つ設問を含むリクエスト
    List<AnswerDto> answers = IntStream.rangeClosed(1, 30).mapToObj(i -> new AnswerDto(i, 5)).toList();
    SaveSheetRequest request = new SaveSheetRequest("山田太郎",
        List.of(new CategoryDto(1, List.of(new QuestionDto(1, answers)))));
    when(service.save(ArgumentMatchers.<SaveSheetRequest>any())).thenReturn(UUID.randomUUID());

    // WHEN & THEN
    mockMvc.perform(post("/api/sheets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("GET /api/sheets/{id} - 指定したIDのスキルシートが正常に取得でき、200 OKが返ること")
  void findById_Success() throws Exception {
    // GIVEN
    UUID targetId = UUID.randomUUID();
    SheetResponse expectedResponse = new SheetResponse("山田太郎", List.of());

    when(service.findById(targetId)).thenReturn(expectedResponse);

    // WHEN & THEN
    mockMvc.perform(get("/api/sheets/{id}", targetId)
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userName").value("山田太郎"));
  }

  @Test
  @DisplayName("GET /api/sheets/{id} - IDがUUIDの形式でない場合は404 Not Foundが返ること")
  void findById_InvalidUuid_Returns404() throws Exception {
    // WHEN & THEN
    mockMvc.perform(get("/api/sheets/{id}", "not-a-uuid")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("POST /api/sheets - JSONとして読み取れないボディの場合は400 Bad Requestが返ること")
  void save_MalformedJson_Returns400() throws Exception {
    // WHEN & THEN
    mockMvc.perform(post("/api/sheets")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{ invalid json"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/sheets/{id} - 未対応のHTTPメソッドの場合は500ではなく405 Method Not Allowedが返ること")
  void findById_UnsupportedMethod_Returns405() throws Exception {
    // WHEN & THEN
    mockMvc.perform(post("/api/sheets/{id}", UUID.randomUUID()))
        .andExpect(status().isMethodNotAllowed());
  }
}
