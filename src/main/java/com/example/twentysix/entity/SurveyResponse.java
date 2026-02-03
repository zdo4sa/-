package com.example.twentysix.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "survey_response")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyResponse {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// どの予約に対するアンケートか紐付ける (NOT NULL)
	@OneToOne
	@JoinColumn(name = "reservation_id", nullable = false, unique = true)
	private Reservation reservation;

	// 1. スタッフの対応（5段階評価: 1~5）
	@Column(nullable = false)
	private Integer staffRating;

	// 2. 設備やサービスの満足度（5段階評価: 1~5）
	@Column(nullable = false)
	private Integer serviceRating;

	// 3. 自由記述（長文OK）
	@Column(columnDefinition = "TEXT")
	private String comment;
}