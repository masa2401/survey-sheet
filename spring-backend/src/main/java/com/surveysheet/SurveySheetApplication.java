package com.surveysheet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SurveySheetApplication {

  public static void main(String[] args) {
    SpringApplication.run(SurveySheetApplication.class, args);
  }

}
