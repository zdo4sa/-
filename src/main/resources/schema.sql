-- 既存テーブル reservation を依存関係ごと削除（再初期化のための安全策）
DROP TABLE IF EXISTS reservation CASCADE;
-- 既存テーブル shift を依存関係ごと削除
DROP TABLE IF EXISTS shift CASCADE;
-- 既存テーブル users を依存関係ごと削除
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS survey_response CASCADE;

-- ユーザを格納するテーブルを作成（ID は連番、認証情報とロールを保持）
CREATE TABLE users (
-- 主キー（PostgreSQL の連番）
id SERIAL PRIMARY KEY,
-- 表示名（必須）
name VARCHAR(50) NOT NULL,
-- ログイン ID としても使うメール（ユニーク制約）
email VARCHAR(255) UNIQUE,
-- 認証用パスワード（必須）※開発中は平文、実運用はハッシュ想定
password VARCHAR(255) NOT NULL,
-- ロール（ADMIN/STAFF/CUSTOMER など）
role VARCHAR(20) NOT NULL,
-- 外部連携用の任意フィールド：LINE ID
line_id VARCHAR(255),
-- 外部連携用の任意フィールド：Google トークン
google_token TEXT
);
-- 予約を格納するテーブルを作成（ユーザ/スタッフへの外部キーを持つ）
CREATE TABLE reservation (
-- 主キー（連番）
id SERIAL PRIMARY KEY,
-- 予約した顧客の FK（NOT NULL）
user_id INT NOT NULL,
-- 担当スタッフの FK（未割り当て可）
staff_id INT,
-- 予約日（必須）
record_date DATE NOT NULL,
-- 予約開始時刻（必須）
time_slot TIME NOT NULL,
-- メニュー名（任意）
menu VARCHAR(255),
-- ステータス（デフォルトは「予約済」）
status VARCHAR(20) DEFAULT '予約済',
-- 顧客 FK 制約（users.id 参照）
FOREIGN KEY (user_id) REFERENCES users(id),
-- スタッフ FK 制約（users.id 参照）
FOREIGN KEY (staff_id) REFERENCES users(id)
);
-- スタッフのシフトを格納するテーブルを作成
CREATE TABLE shift (
-- 主キー（連番）
id SERIAL PRIMARY KEY,
-- 対象スタッフの FK（必須）
staff_id INT NOT NULL,
-- シフト日（必須）
record_date DATE NOT NULL,
-- シフト開始時刻（NULL 可：柔軟性確保）
start_time TIME,
-- シフト終了時刻（NULL 可）
end_time TIME,
-- スタッフ FK 制約（users.id 参照）
FOREIGN KEY (staff_id) REFERENCES users(id)
);
CREATE TABLE "survey_response" (
    -- 主キー（連番）
    id SERIAL PRIMARY KEY,
    -- 紐づく予約の ID（ユニーク制約：1つの予約に1つのアンケート）
    reservation_id INT NOT NULL UNIQUE,
    -- スタッフ対応評価（1～5の整数）
    staff_rating INT NOT NULL,
    -- 設備・サービス評価（1～5の整数）
    service_rating INT NOT NULL,
    -- 自由記述（★ここを追加）
    comment TEXT,
    -- 外部キー（★ここを追加）
    FOREIGN KEY (reservation_id) REFERENCES reservation(id)
);