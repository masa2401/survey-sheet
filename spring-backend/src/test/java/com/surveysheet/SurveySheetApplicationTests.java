package com.surveysheet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "survey.dir=../surveys/skill-sheet")
@Import(TestcontainersConfiguration.class)
class SurveySheetApplicationTests {

  @Test
  void contextLoads() {
  }

}
