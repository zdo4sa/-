-- 顧客 A (パスワード: password) 
-- ※このハッシュ値は標準的な計算結果です
INSERT INTO users (name, email, password, role) 
VALUES ('顧客 A', 'customerA@example.com', '$2a$10$09kEZUelJqzj5z3lrxWgZOdJLjkTMolYZOznX/WMOVwQ5tzjaOnb6', 'ROLE_CUSTOMER');

-- スタッフ B
INSERT INTO users (name, email, password, role) 
VALUES ('スタッフ B', 'staffB@example.com', '$2a$10$09kEZUelJqzj5z3lrxWgZOdJLjkTMolYZOznX/WMOVwQ5tzjaOnb6', 'ROLE_STAFF');

-- 管理者 C
INSERT INTO users (name, email, password, role) 
VALUES ('管理者 C', 'adminC@example.com', '$2a$10$09kEZUelJqzj5z3lrxWgZOdJLjkTMolYZOznX/WMOVwQ5tzjaOnb6', 'ROLE_ADMIN');

-- 顧客 D
INSERT INTO users (name, email, password, role) 
VALUES ('顧客 D', 'customeDr@example.com', '$2a$10$09kEZUelJqzj5z3lrxWgZOdJLjkTMolYZOznX/WMOVwQ5tzjaOnb6', 'ROLE_CUSTOMER');

-- スタッフ E
INSERT INTO users (name, email, password, role) 
VALUES ('スタッフ E', 'staffE@example.com', '$2a$10$09kEZUelJqzj5z3lrxWgZOdJLjkTMolYZOznX/WMOVwQ5tzjaOnb6', 'ROLE_STAFF');

-- 2. シフト登録（staff_id取得用。emailは上記と一致させること）
INSERT INTO shift (staff_id, record_date, start_time, end_time) VALUES
((SELECT id FROM users WHERE email ='staffB@example.com'), CURRENT_DATE, '09:00:00', '17:00:00');

-- 3. 【重要】アンケートの「親」になる予約データを先に入れる
INSERT INTO reservation (user_id, staff_id, record_date, time_slot, menu, status) VALUES
(
  (SELECT id FROM users WHERE email = 'customerA@example.com'),
  (SELECT id FROM users WHERE email = 'staffB@example.com'),
  CURRENT_DATE - INTERVAL '1 day', -- 昨日の日付（過去の予約にする）
  '10:00:00',
  'カット',
  '予約済'
);

-- 4. 最後にアンケートを登録する（これで ID が見つかるようになります）
INSERT INTO "survey_response" (reservation_id, staff_rating, service_rating, comment) VALUES
(
  (SELECT id FROM reservation 
   WHERE user_id = (SELECT id FROM users WHERE email = 'customerA@example.com') 
   LIMIT 1), 
  5, 
  4, 
  '非常に丁寧な接客で満足しました！'
);