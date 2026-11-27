package com.surveysheet.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "skill_sheets")
@Getter
@Setter
@NoArgsConstructor
public class SkillSheet {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "user_name", nullable = false)
  private String userName;

  // 機能拡張時利用予定トークン
  @Column(name = "share_token", unique = true)
  private String shareToken;

  @CreationTimestamp
  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @OneToMany(mappedBy = "skillSheet", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SheetCategory> categories = new ArrayList<>();
}
