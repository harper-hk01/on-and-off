-- =====================================================
-- on-and-off 메신저 DB 생성 스크립트

-- <주의>
-- 맨 위의 DROP DATABASE는 on-and-off DB를 통째로 지운다. 
-- 그 안에 쌓인 회원·메시지 데이터 모두 사라진다. 
-- 처음 세팅할 때, 또는 설계가 바뀌어 처음부터 다시 만들 때만 실행해야 한다. 
--
-- 실행 방법
--   방법 1) MySQL Workbench: 이 파일을 열고 번개 아이콘(전체 실행)
--   방법 2) 명령 프롬프트:  mysql -u root -p < db/schema.sql
--  방법 3) HeidiSQL : 이 파일을 불러와서 실행한다. F5는 눌러 on-and-off DB 안에 테이블 5개가 있는지 본다. 
--
-- DB 이름에 '-'가 들어 있어서 SQL 안에서는 반드시 `on-and-off`처럼
-- 백틱으로 감싸야 합니다.
-- =====================================================

-- 1) DB를 지우고 새로 만들기
--    utf8mb4 = 한글과 이모지까지 저장되는 문자 방식
DROP DATABASE IF EXISTS `on-and-off`;
CREATE DATABASE `on-and-off`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;
USE `on-and-off`;

-- 2) users: 회원
CREATE TABLE users (
  user_id        BIGINT       NOT NULL AUTO_INCREMENT,
  login_id       VARCHAR(20)  NOT NULL,
  password       VARCHAR(100) NOT NULL,
  nickname       VARCHAR(20)  NOT NULL,
  status_message VARCHAR(100) NULL DEFAULT NULL,
  status         ENUM('ONLINE','AWAY','OFFLINE') NOT NULL DEFAULT 'OFFLINE',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  UNIQUE KEY uk_users_login_id (login_id)
);

-- 3) friends: 친구 관계 (A가 B를 추가하면 A→B, B→A 두 줄)
CREATE TABLE friends (
  user_id    BIGINT   NOT NULL,
  friend_id  BIGINT   NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, friend_id),
  CONSTRAINT fk_friends_user   FOREIGN KEY (user_id)   REFERENCES users (user_id),
  CONSTRAINT fk_friends_friend FOREIGN KEY (friend_id) REFERENCES users (user_id)
);

-- 4) chat_rooms: 대화방 (DAILY = 1:1 방, WORK = 그룹방)
--    pinned_message_id의 FK는 messages 테이블이 생긴 뒤 7)에서 추가
CREATE TABLE chat_rooms (
  room_id           BIGINT      NOT NULL AUTO_INCREMENT,
  room_type         ENUM('DAILY','WORK') NOT NULL,
  room_name         VARCHAR(50) NULL DEFAULT NULL,
  pinned_message_id BIGINT      NULL DEFAULT NULL,
  created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (room_id)
);

-- 5) messages: 메시지
CREATE TABLE messages (
  message_id BIGINT   NOT NULL AUTO_INCREMENT,
  room_id    BIGINT   NOT NULL,
  sender_id  BIGINT   NOT NULL,
  content    TEXT     NOT NULL,
  sent_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (message_id),
  KEY idx_messages_room (room_id, message_id),
  CONSTRAINT fk_messages_room   FOREIGN KEY (room_id)   REFERENCES chat_rooms (room_id),
  CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (user_id)
);

-- 6) room_members: 대화방 참여자 (회원↔대화방 연결 테이블)
CREATE TABLE room_members (
  room_id              BIGINT   NOT NULL,
  user_id              BIGINT   NOT NULL,
  last_read_message_id BIGINT   NULL DEFAULT NULL,
  joined_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (room_id, user_id),
  KEY idx_room_members_user (user_id),
  CONSTRAINT fk_room_members_room FOREIGN KEY (room_id) REFERENCES chat_rooms (room_id),
  CONSTRAINT fk_room_members_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

-- 7) 공지 FK: chat_rooms와 messages가 서로를 가리키므로 마지막에 추가
ALTER TABLE chat_rooms
  ADD CONSTRAINT fk_chat_rooms_pinned
  FOREIGN KEY (pinned_message_id) REFERENCES messages (message_id);

-- 끝. 테스트 데이터가 필요하면 이어서 db/sample_data.sql을 실행하세요.
