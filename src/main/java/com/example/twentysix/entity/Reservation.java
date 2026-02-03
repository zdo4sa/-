package com.example.twentysix.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// このクラスが JPA エンティティであることを示す
@Entity
// 対応するテーブル名を reservation に固定
@Table(name = "reservation")
// Lombok：getter/setter/toString/equals/hashCode を自動生成
@Data
// Lombok：引数なしコンストラクタを自動生成
@NoArgsConstructor
// Lombok：全フィールドを引数に持つコンストラクタを自動生成
@AllArgsConstructor
// 予約を表すドメインエンティティ
public class Reservation {
	// 主キーであることを示す
	@Id
	// 主キー採番戦略：DB の IDENTITY（PostgreSQL の serial/identity と相性良し）
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	// 予約 ID（PK）
	private Long id;
	// 多対一で users テーブル（User）に紐づく（予約の所有者＝顧客）
	@ManyToOne
	// 外部キー列 user_id に結合。NOT NULL 制約を付与
	@JoinColumn(name = "user_id", nullable = false)
	// 予約した顧客
	private User user;
	// 多対一でスタッフ（担当者）に紐づく（null 許容：未割当を許す）
	@ManyToOne
	// 外部キー列 staff_id に結合（nullable デフォルトは true）
	@JoinColumn(name = "staff_id")
	// 担当スタッフ（未割当の場合は null）
	private User staff;
	// 予約日を record_date 列にマッピング。NOT NULL 制約
	@Column(name = "record_date", nullable = false)
	//@Column(name = "record_date", nullable = false)
	// 予約日（年月日）
	private LocalDate recordDate; // date から recordDate に変更
	// 予約時間枠を time_slot 列にマッピング。NOT NULL 制約
	@Column(name = "time_slot", nullable = false)
	// 予約時間（開始時刻）
	private LocalTime timeSlot;
	//メニュー名（任意文字列）
	private String menu;
	//予約ステータスの初期値を「予約済」に設定（DB デフォルトとも一致）
	private String status = "予約済"; // default status
	@OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true) // ★これが必要
	@lombok.ToString.Exclude // 循環参照によるエラーを防ぐために必須

	private SurveyResponse surveyResponse;

	public SurveyResponse getSurveyResponse() {
		return this.surveyResponse;
	}

	public void setSurveyResponse(SurveyResponse surveyResponse) {
		this.surveyResponse = surveyResponse;
	}

}