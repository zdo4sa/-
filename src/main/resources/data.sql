INSERT INTO users (name, email, password, role) VALUES
('顧客 A','customerA@example.com','password','CUSTOMER'),
('スタッフ B','staffB@example.com','password','STAFF'),
('管理者 C','adminC@example.com','password','ADMIN'),
('顧客 D','customeDr@example.com','password','CUSTOMER'),
('スタッフ E','staffE@example.com','password','STAFF');

-- 2. シフト登録
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