package com.surveysheet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sheet_answers")
@Getter
@Setter
@NoArgsConstructor

public class SheetAnswer {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sheet_answers_seq")
  @SequenceGenerator(name = "sheet_answers_seq", sequenceName = "sheet_answers_id_seq", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sheet_category_id", nullable = false)
  private SheetCategory sheetCategory;

  @Column(name = "question_id")
  private Integer questionId;

  @Column(name = "answer_id")
  private Integer answerId;

  private Integer value;
}
