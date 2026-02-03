package com.example.twentysix.entity;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// このクラスが JPA エンティティであることを示す
@Entity
// テーブル名を users に指定（PostgreSQL の予約語回避と複数形に合わせる）
@Table(name = "shift")
// Lombok：getter/setter/toString/equals/hashCode を自動生成
@Data
// Lombok：引数なしコンストラクタを自動生成
@NoArgsConstructor
// Lombok：全フィールド引数コンストラクタを自動生成
@AllArgsConstructor
// システムのユーザ（顧客/スタッフ/管理者）を表すエンティティ
public class Shift {
	// 主キーの指定
	@Id
	// 採番戦略：DB の IDENTITY を利用
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	// ユーザ ID（PK）
	private Long id;
	// NOT NULL 制約の付いたユーザ名
	@Column(name = "record_date", nullable = false)
	@org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd")
	//追加変更
	private LocalDate recordDate;
	@ManyToOne
	@jakarta.persistence.JoinColumn(name = "staff_id", nullable = false)
	@lombok.ToString.Exclude // これを追加

	private User staff;
	@org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.TIME)
	private LocalTime startTime;
	@org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.TIME)
	private LocalTime endTime;
	//private LocalTime startTime;
	//private String staff;を変えた
	//private LocalDate date;を変えた

}