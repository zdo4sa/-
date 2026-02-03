package com.example.twentysix.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//このクラスが JPA エンティティであることを示す
@Entity
//テーブル名を users に指定（PostgreSQL の予約語回避と複数形に合わせる）
@Table(name = "users")
//Lombok：getter/setter/toString/equals/hashCode を自動生成
@Data
//Lombok：引数なしコンストラクタを自動生成
@NoArgsConstructor
//Lombok：全フィールド引数コンストラクタを自動生成
@AllArgsConstructor
//システムのユーザ（顧客/スタッフ/管理者）を表すエンティティ
public class User {
	//主キーの指定
	@Id
	//採番戦略：DB の IDENTITY を利用
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	//ユーザ ID（PK）
	private Long id;
	//NOT NULL 制約の付いたユーザ名
	@Column(nullable = false)
	//表示名（顧客名・スタッフ名など）
	private String name;
	//一意制約の付いたメールアドレス（ログイン ID として使用）
	@Column(unique = true)
	//メールアドレス（認証時の username）
	private String email;
	//NOT NULL 制約の付いたパスワード
	@Column(nullable = false)
	//パスワード（開発中は平文／本番は BCrypt ハッシュ推奨）
	private String password;
	//NOT NULL 制約の付いたロール名（"ADMIN" / "STAFF" / "CUSTOMER"）
	@Column(nullable = false)
	//権限ロール（Spring Security の .roles() へ渡す想定）
	private String role;
	//LINE 連携用の識別子（任意）
	@Column(name = "line_id")
	//LINE ID（外部連携予定の拡張フィールド）
	private String lineId;
	//Google 連携用のトークン（任意）
	@Column(name = "google_token")
	//Google OAuth 連携トークン等の格納想定
	private String googleToken;
}
