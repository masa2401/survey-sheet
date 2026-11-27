package com.surveysheet.controller;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.surveysheet.service.DefinitionService;
import com.surveysheet.service.definition.Definition;
import com.surveysheet.service.definition.Display;

@WebMvcTest(DefinitionController.class)
class DefinitionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DefinitionService service;

  @Test
  @DisplayName("GET /api/survey - 版・設問・表示設定・入力の上限を返す")
  void returnsDefinition() throws Exception {
    Definition definition = new Definition("abc",
        new Definition.Survey(List.of(
            new Definition.Group("common", "共通", false, null,
                List.of(new Definition.Question(1, "Q", "案内", List.of(new Definition.Item(1, "Slack"))))),
            new Definition.Group("eng", "エンジニア", true, "開発者向け", List.of()))),
        new Display("アンケート", new Display.Scale("評価", List.of("1", "2", "3", "4", "5")), Map.of(),
            new Display.Top(null, "見出し"), new Display.Result("{name}のアンケート"),
            new Display.Csv("{name}", "評価項目")));
    when(service.getDefinition()).thenReturn(definition);
    when(service.getUserNameMaxLength()).thenReturn(20);

    mockMvc.perform(get("/api/survey"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value("abc"))
        .andExpect(jsonPath("$.survey.groups[0].selectable").value(false))
        .andExpect(jsonPath("$.survey.groups[0]", hasKey("description")))
        .andExpect(jsonPath("$.survey.groups[0].description").value(nullValue()))
        .andExpect(jsonPath("$.survey.groups[0].questions[0].items[0].id").value(1))
        .andExpect(jsonPath("$.survey.groups[0].questions[0].items[0].label").value("Slack"))
        .andExpect(jsonPath("$.survey.groups[1].description").value("開発者向け"))
        .andExpect(jsonPath("$.display.top", hasKey("banner")))
        .andExpect(jsonPath("$.display.top.banner").value(nullValue()))
        .andExpect(jsonPath("$.display.csv.itemHeading").value("評価項目"))
        .andExpect(jsonPath("$.limits.userNameMaxLength").value(20));
  }
}
