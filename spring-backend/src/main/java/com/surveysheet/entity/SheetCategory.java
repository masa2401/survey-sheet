package com.surveysheet.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sheet_categories")
@Getter
@Setter
@NoArgsConstructor

public class SheetCategory {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sheet_categories_seq")
  @SequenceGenerator(name = "sheet_categories_seq", sequenceName = "sheet_categories_id_seq", allocationSize = 1)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sheet_id", nullable = false)
  private SkillSheet skillSheet;

  @Column(name = "category_id")
  private Integer categoryId;

  @OneToMany(mappedBy = "sheetCategory", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SheetAnswer> answers = new ArrayList<>();
}
