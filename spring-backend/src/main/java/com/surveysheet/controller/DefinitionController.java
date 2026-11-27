package com.surveysheet.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.surveysheet.dto.response.DefinitionResponse;
import com.surveysheet.service.DefinitionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/survey")
@RequiredArgsConstructor
public class DefinitionController {
  private final DefinitionService definitionService;

  @GetMapping
  public DefinitionResponse get() {
    return DefinitionResponse.of(definitionService.getDefinition(), definitionService.getUserNameMaxLength());
  }
}
