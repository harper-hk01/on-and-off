-- =====================================================
-- on-and-off 테스트용 데이터
-- 반드시 db/schema.sql을 먼저 실행한 직후(빈 DB 상태)에 실행하세요.
--   (번호 1, 2, 3을 직접 가정하고 넣기 때문에, 이미 데이터가 있으면 번호가 어긋납니다)
--
-- 실행 방법: Workbench에서 열고 전체 실행, 또는
--            mysql -u root -p < db/sample_data.sql
--
-- 테스트 계정 (비밀번호는 모두 1234)
--   user_id 1  chulsoo   철수
--   user_id 2  younghee  영희
--   user_id 3  minsu     민수
-- =====================================================
USE `on-and-off`;

-- 1) 회원 3명 (status는 기본값 OFFLINE)
INSERT INTO users (login_id, password, nickname, status_message) VALUES
  ('chulsoo',  '1234', '철수', '오늘도 화이팅'),
  ('younghee', '1234', '영희', NULL),
  ('minsu',    '1234', '민수', '회의 중엔 연락 주세요');

-- 2) 친구 관계 (팀 규칙: 한 번 추가하면 양방향 두 줄)
--    철수-영희, 철수-민수는 친구 / 영희-민수는 친구 아님 (친구 추가 테스트용)
INSERT INTO friends (user_id, friend_id) VALUES
  (1, 2), (2, 1),
  (1, 3), (3, 1);

-- 3) 대화방 2개
--    room 1: 철수-영희 1:1 방 (DAILY, 이름 없음)
--    room 2: 철수·영희·민수 그룹방 (WORK)
INSERT INTO chat_rooms (room_type, room_name) VALUES
  ('DAILY', NULL),
  ('WORK',  '팀 프로젝트방');

-- 4) 방 참여자
INSERT INTO room_members (room_id, user_id) VALUES
  (1, 1), (1, 2),
  (2, 1), (2, 2), (2, 3);

-- 5) 메시지 (message_id 1~5)
INSERT INTO messages (room_id, sender_id, content) VALUES
  (1, 1, '영희야 안녕!'),
  (1, 2, '응 안녕 철수야'),
  (2, 1, '오늘 9시에 회의해요'),
  (2, 3, '넵 알겠습니다'),
  (2, 2, '자료는 노션에 올려 둘게요');

-- 6) 읽음 표시: 철수는 1번 방을 1번 메시지까지만 읽음 → 1번 방에 NEW 표시가 떠야 함
UPDATE room_members SET last_read_message_id = 1 WHERE room_id = 1 AND user_id = 1;

-- 7) 그룹방 공지: 3번 메시지('오늘 9시에 회의해요')를 공지로 고정
UPDATE chat_rooms SET pinned_message_id = 3 WHERE room_id = 2;
