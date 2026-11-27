package com.surveysheet.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.surveysheet.entity.SheetAnswer;

public interface SheetAnswerRepository extends JpaRepository<SheetAnswer, Long> {
}