-- =========================================================
-- Phoenix DB (with reservation channel + samples)
-- =========================================================
DROP DATABASE IF EXISTS phoenix;
CREATE DATABASE phoenix
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE phoenix;

-- ---------------------- 구역 테이블 ----------------------
CREATE TABLE zones (
  zno   INT AUTO_INCREMENT PRIMARY KEY,
  zname VARCHAR(50) NOT NULL,
  price INT NOT NULL
);

-- ---------------------- 회원 테이블 ----------------------
CREATE TABLE members (
  mno               INT AUTO_INCREMENT PRIMARY KEY,
  mid               VARCHAR(50)  NULL UNIQUE,
  password_hash     VARCHAR(255) NULL,
  mname             VARCHAR(50)  NOT NULL,
  mphone            VARCHAR(13)  NULL UNIQUE,  -- NULL 허용 + UNIQUE
  birthdate         DATE         NOT NULL,
  email             VARCHAR(100) NOT NULL UNIQUE,
  create_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,

  provider          VARCHAR(50),
  provider_id       VARCHAR(100),

  pno               INT NULL,  -- 선호 선수

  status            ENUM('active','dormant','withdrawn') NOT NULL DEFAULT 'active',
  exchange          BOOLEAN NOT NULL DEFAULT TRUE,
  email_verified    BOOLEAN NOT NULL DEFAULT FALSE,
  last_status_change TIMESTAMP NULL
);

-- (명시적 ALTER가 필요 없다면 아래 줄은 생략 가능)
ALTER TABLE members MODIFY COLUMN mphone VARCHAR(13) NULL UNIQUE;

-- ---------------------- 시니어 회원 뷰 ----------------------
CREATE OR REPLACE VIEW member_view AS
SELECT m.*,
       (TIMESTAMPDIFF(YEAR, m.birthdate, CURDATE()) >= 65) AS is_senior
FROM members m;

-- ---------------------- 좌석 테이블 ----------------------
CREATE TABLE seats (
  sno      INT AUTO_INCREMENT PRIMARY KEY,
  zno      INT NOT NULL,
  seatName VARCHAR(30) NOT NULL,
  senior   BOOLEAN DEFAULT FALSE,
  CONSTRAINT fk_seats_zone FOREIGN KEY (zno) REFERENCES zones(zno)
);
CREATE INDEX idx_seats_zone ON seats(zno);
CREATE INDEX idx_seats_zone_senior ON seats(zno, senior);
CREATE UNIQUE INDEX ux_seats_zno_seatname ON seats(zno, seatName);

-- ---------------------- 예매 테이블 ----------------------
-- FK 유지: 회원, 좌석은 반드시 존재해야 함
-- gno는 외부(CSV/크롤링)와 매칭하므로 FK 제거
CREATE TABLE reservations (
  rno         INT AUTO_INCREMENT PRIMARY KEY,
  mno         INT NOT NULL,
  sno         INT NOT NULL,
  gno         INT NOT NULL,
  reserved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  status      ENUM('reserved','cancelled') NOT NULL DEFAULT 'reserved',
  channel     ENUM('general','senior') NOT NULL DEFAULT 'general',
  CONSTRAINT fk_res_mno FOREIGN KEY (mno) REFERENCES members(mno),
  CONSTRAINT fk_res_sno FOREIGN KEY (sno) REFERENCES seats(sno)
);
-- 조회 최적화 인덱스
CREATE INDEX idx_res_gno_status_channel ON reservations(gno, status, channel);
CREATE INDEX idx_res_mno_gno ON reservations(mno, gno);
CREATE INDEX idx_res_sno_gno ON reservations(sno, gno);

-- ---------------------- 티켓 테이블 ----------------------
CREATE TABLE tickets (
  tno         INT AUTO_INCREMENT PRIMARY KEY,
  rno         INT NOT NULL,
  ticket_code VARCHAR(100) NOT NULL UNIQUE,
  issued_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  valid       BOOLEAN   NOT NULL DEFAULT TRUE,
  price       INT       NOT NULL,
  ticket_uuid VARCHAR(20),
  CONSTRAINT fk_tickets_rno FOREIGN KEY (rno) REFERENCES reservations(rno)
);
CREATE INDEX idx_tickets_rno ON tickets(rno);

-- ---------------------- 예매 교환 테이블 ----------------------
CREATE TABLE reservation_exchanges (
  exno        INT AUTO_INCREMENT PRIMARY KEY,
  from_rno    INT NOT NULL,
  to_rno      INT NOT NULL,
  status      ENUM('pending','approved','rejected') NOT NULL DEFAULT 'pending',
  requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  responded_at TIMESTAMP NULL,
  CONSTRAINT fk_ex_from FOREIGN KEY (from_rno) REFERENCES reservations(rno),
  CONSTRAINT fk_ex_to   FOREIGN KEY (to_rno)   REFERENCES reservations(rno)
);

-- ---------------------- 자동배정 로그 테이블 ----------------------
CREATE TABLE auto_assign_log (
  lno          INT AUTO_INCREMENT PRIMARY KEY,
  mno          INT NOT NULL,
  gno          INT NOT NULL,
  assigned_zno INT,
  reason       VARCHAR(255),
  create_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------- Auto-increment bases ----------------------
ALTER TABLE zones                AUTO_INCREMENT = 10001;
ALTER TABLE members              AUTO_INCREMENT = 20001;
ALTER TABLE seats                AUTO_INCREMENT = 30001;
ALTER TABLE reservations         AUTO_INCREMENT = 40001;
ALTER TABLE tickets              AUTO_INCREMENT = 50001;
ALTER TABLE reservation_exchanges AUTO_INCREMENT = 60001;
ALTER TABLE auto_assign_log      AUTO_INCREMENT = 70001;

-- =========================================================
-- 샘플 데이터
-- =========================================================

-- 구역
INSERT INTO zones(zname, price) VALUES
('1루 일반석', 30000),
('3루 일반석', 30000),
('외야석', 20000),
('내야 지정석', 50000),
('프리미엄석', 80000),
('시니어 전용석', 15000),
('스카이박스', 120000),
('응원석', 35000),
('가족석', 60000),
('커플석', 55000);

-- 회원
INSERT INTO members (
  mid, password_hash, mname, mphone, birthdate, email,
  provider, provider_id, pno, email_verified
) VALUES
('admin','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','관리자','010-0000-0000','2025-10-01','admin@test.com',NULL,NULL,0 ,TRUE),
('user1','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','홍길동','010-1111-1111','1960-05-10','user1@test.com',NULL,NULL,10,TRUE),
('user2','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','이순신','010-1111-1112','1990-02-20','user2@test.com',NULL,NULL,11,TRUE),
('user3','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','강감찬','010-1111-1113','1985-07-15','user3@test.com',NULL,NULL,12,TRUE),
('user4','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','유관순','010-1111-1114','1970-11-05','user4@test.com',NULL,NULL,13,TRUE),
('user5','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','안중근','010-1111-1115','2000-03-22','user5@test.com',NULL,NULL,14,TRUE),
('user6','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','윤봉길','010-1111-1116','1962-09-18','user6@test.com',NULL,NULL,15,TRUE),
('user7','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','정몽주','010-1111-1117','1995-12-01','user7@test.com',NULL,NULL,16,TRUE),
('user8','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','신사임당','010-1111-1118','1988-06-25','user8@test.com',NULL,NULL,17,TRUE),
('user9','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','세종대왕','010-1111-1119','1955-08-30','user9@test.com',NULL,NULL,18,TRUE),
('user10','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','장영실','010-1111-1120','1999-01-10','user10@test.com',NULL,NULL,19,TRUE);

-- 좌석 (각 존 A1~A10 시니어, B/C 일반)
-- 겨레석 (zno = 10001) → sno 30001~30030
INSERT INTO seats(zno, seatName, senior) VALUES
(10001,'A1',TRUE),(10001,'A2',TRUE),(10001,'A3',TRUE),(10001,'A4',TRUE),(10001,'A5',TRUE),
(10001,'A6',TRUE),(10001,'A7',TRUE),(10001,'A8',TRUE),(10001,'A9',TRUE),(10001,'A10',TRUE),
(10001,'B1',FALSE),(10001,'B2',FALSE),(10001,'B3',FALSE),(10001,'B4',FALSE),(10001,'B5',FALSE),
(10001,'B6',FALSE),(10001,'B7',FALSE),(10001,'B8',FALSE),(10001,'B9',FALSE),(10001,'B10',FALSE),
(10001,'C1',FALSE),(10001,'C2',FALSE),(10001,'C3',FALSE),(10001,'C4',FALSE),(10001,'C5',FALSE),
(10001,'C6',FALSE),(10001,'C7',FALSE),(10001,'C8',FALSE),(10001,'C9',FALSE),(10001,'C10',FALSE);

-- 연우석 (zno = 10002) → sno 30031~30060
INSERT INTO seats(zno, seatName, senior) VALUES
(10002,'A1',TRUE),(10002,'A2',TRUE),(10002,'A3',TRUE),(10002,'A4',TRUE),(10002,'A5',TRUE),
(10002,'A6',TRUE),(10002,'A7',TRUE),(10002,'A8',TRUE),(10002,'A9',TRUE),(10002,'A10',TRUE),
(10002,'B1',FALSE),(10002,'B2',FALSE),(10002,'B3',FALSE),(10002,'B4',FALSE),(10002,'B5',FALSE),
(10002,'B6',FALSE),(10002,'B7',FALSE),(10002,'B8',FALSE),(10002,'B9',FALSE),(10002,'B10',FALSE),
(10002,'C1',FALSE),(10002,'C2',FALSE),(10002,'C3',FALSE),(10002,'C4',FALSE),(10002,'C5',FALSE),
(10002,'C6',FALSE),(10002,'C7',FALSE),(10002,'C8',FALSE),(10002,'C9',FALSE),(10002,'C10',FALSE);

-- 성호석 (zno = 10003) → sno 30061~30090
INSERT INTO seats(zno, seatName, senior) VALUES
(10003,'A1',TRUE),(10003,'A2',TRUE),(10003,'A3',TRUE),(10003,'A4',TRUE),(10003,'A5',TRUE),
(10003,'A6',TRUE),(10003,'A7',TRUE),(10003,'A8',TRUE),(10003,'A9',TRUE),(10003,'A10',TRUE),
(10003,'B1',FALSE),(10003,'B2',FALSE),(10003,'B3',FALSE),(10003,'B4',FALSE),(10003,'B5',FALSE),
(10003,'B6',FALSE),(10003,'B7',FALSE),(10003,'B8',FALSE),(10003,'B9',FALSE),(10003,'B10',FALSE),
(10003,'C1',FALSE),(10003,'C2',FALSE),(10003,'C3',FALSE),(10003,'C4',FALSE),(10003,'C5',FALSE),
(10003,'C6',FALSE),(10003,'C7',FALSE),(10003,'C8',FALSE),(10003,'C9',FALSE),(10003,'C10',FALSE);

-- 찬영석 (zno = 10004) → sno 30091~30120
INSERT INTO seats(zno, seatName, senior) VALUES
(10004,'A1',TRUE),(10004,'A2',TRUE),(10004,'A3',TRUE),(10004,'A4',TRUE),(10004,'A5',TRUE),
(10004,'A6',TRUE),(10004,'A7',TRUE),(10004,'A8',TRUE),(10004,'A9',TRUE),(10004,'A10',TRUE),
(10004,'B1',FALSE),(10004,'B2',FALSE),(10004,'B3',FALSE),(10004,'B4',FALSE),(10004,'B5',FALSE),
(10004,'B6',FALSE),(10004,'B7',FALSE),(10004,'B8',FALSE),(10004,'B9',FALSE),(10004,'B10',FALSE),
(10004,'C1',FALSE),(10004,'C2',FALSE),(10004,'C3',FALSE),(10004,'C4',FALSE),(10004,'C5',FALSE),
(10004,'C6',FALSE),(10004,'C7',FALSE),(10004,'C8',FALSE),(10004,'C9',FALSE),(10004,'C10',FALSE);

-- 중앙테이블석 (zno = 10005) → sno 30121~30150
INSERT INTO seats(zno, seatName, senior) VALUES
(10005,'A1',TRUE),(10005,'A2',TRUE),(10005,'A3',TRUE),(10005,'A4',TRUE),(10005,'A5',TRUE),
(10005,'A6',TRUE),(10005,'A7',TRUE),(10005,'A8',TRUE),(10005,'A9',TRUE),(10005,'A10',TRUE),
(10005,'B1',FALSE),(10005,'B2',FALSE),(10005,'B3',FALSE),(10005,'B4',FALSE),(10005,'B5',FALSE),
(10005,'B6',FALSE),(10005,'B7',FALSE),(10005,'B8',FALSE),(10005,'B9',FALSE),(10005,'B10',FALSE),
(10005,'C1',FALSE),(10005,'C2',FALSE),(10005,'C3',FALSE),(10005,'C4',FALSE),(10005,'C5',FALSE),
(10005,'C6',FALSE),(10005,'C7',FALSE),(10005,'C8',FALSE),(10005,'C9',FALSE),(10005,'C10',FALSE);

-- 외야자유석 (zno = 10006) → sno 30151~30180
INSERT INTO seats(zno, seatName, senior) VALUES
(10006,'A1',TRUE),(10006,'A2',TRUE),(10006,'A3',TRUE),(10006,'A4',TRUE),(10006,'A5',TRUE),
(10006,'A6',TRUE),(10006,'A7',TRUE),(10006,'A8',TRUE),(10006,'A9',TRUE),(10006,'A10',TRUE),
(10006,'B1',FALSE),(10006,'B2',FALSE),(10006,'B3',FALSE),(10006,'B4',FALSE),(10006,'B5',FALSE),
(10006,'B6',FALSE),(10006,'B7',FALSE),(10006,'B8',FALSE),(10006,'B9',FALSE),(10006,'B10',FALSE),
(10006,'C1',FALSE),(10006,'C2',FALSE),(10006,'C3',FALSE),(10006,'C4',FALSE),(10006,'C5',FALSE),
(10006,'C6',FALSE),(10006,'C7',FALSE),(10006,'C8',FALSE),(10006,'C9',FALSE),(10006,'C10',FALSE);

-- ====================== 예약 (channel 포함) ======================
-- ⚠️ rno 40001..40010 매핑이 유지되도록 "순서"는 원본과 동일
INSERT INTO reservations (mno, sno, gno, status, channel) VALUES
-- 회원 20001 (3건, 1건 취소)
(20001, 30001,   1, 'reserved',  'general'),   -- rno 40001
(20001, 30002,   1, 'cancelled', 'general'),   -- rno 40002
(20001, 30003,   1, 'reserved',  'general'),   -- rno 40003

-- 회원 20002 (5건, 이 중 2건은 gno=146 → 시니어 표본)
(20002, 30031,   2, 'reserved',  'general'),   -- rno 40004
(20002, 30032,   2, 'reserved',  'general'),   -- rno 40005
(20002, 30033,   2, 'cancelled', 'general'),   -- rno 40006
(20002, 30003, 146, 'reserved',  'senior'),    -- rno 40007 ★ 시니어 예매
(20002, 30004, 146, 'reserved',  'senior'),    -- rno 40008 ★ 시니어 예매

-- 회원 20003 (5건, 일부 시니어 예매 표본)
(20003, 30061,   3, 'reserved',  'general'),   -- rno 40009
(20003, 30062,   3, 'cancelled', 'general'),   -- rno 40010
(20003, 30063,   3, 'reserved',  'general'),
(20003, 30005, 146, 'reserved',  'senior'),    -- 시니어 예매
(20003, 30006, 146, 'reserved',  'senior'),

-- 회원 20004 (1건)
(20004, 30091,   4, 'reserved',  'general');

-- ====================== 티켓 (rno 40001..40010) ======================
INSERT INTO tickets (rno, ticket_code, valid, price, ticket_uuid) VALUES
(40001,'/upload/20251028_3d6003_qr.png',TRUE ,30000,'a5bc97'),
(40002,'/upload/20251028_4e0eb3_qr.png',TRUE ,30000,'cbb5ae'),
(40003,'/upload/20251028_5dc27c_qr.png',TRUE ,20000,'d81a3e'),
(40004,'/upload/20251028_7f4a7e_qr.png',TRUE ,20000,'3150a1'),
(40005,'/upload/20251028_4489a0_qr.png',TRUE ,50000,'c27143'),
(40006,'/upload/20251028_769745_qr.png',TRUE ,50000,'ce2978'),
(40007,'/upload/20251028_a450b9_qr.png',TRUE ,80000,'8c47ea'),
(40008,'/upload/20251028_b10462_qr.png',TRUE ,15000,'ca73c6'),
(40009,'/upload/20251028_ba3a3d_qr.png',TRUE ,120000,'650139'),
(40010,'/upload/20251028_f88e45_qr.png',FALSE,35000,'1d552a');

-- ====================== 예매 교환 (예시) ======================
INSERT INTO reservation_exchanges (from_rno, to_rno, status) VALUES
(40001,40002,'pending'),
(40003,40004,'pending'),
(40005,40006,'approved'),
(40007,40008,'approved'),
(40009,40010,'rejected'),
(40002,40003,'pending'),
(40004,40005,'pending'),
(40006,40007,'approved'),
(40008,40009,'rejected'),
(40001,40010,'pending');

-- ====================== 자동배정 로그 (샘플) ======================
INSERT INTO auto_assign_log(mno, gno, assigned_zno, reason) VALUES
(20001,1001,10001,'시니어 회원 우선 배정'),
(20002,1001,10002,'선호 구역 자동 배정'),
(20003,1002,10003,'남은 좌석 랜덤 배정'),
(20004,1002,10004,'선호 구역 매진 → 인접 구역 배정'),
(20005,1003,10005,'프리미엄 좌석 불가로 일반석 배정'),
(20006,1003,10006,'시니어 전용석 자동 배정'),
(20007,1004,10007,'랜덤 배정'),
(20008,1004,10008,'기존 예약 취소로 재배정'),
(20009,1005,10009,'스카이박스 매진 → 가족석 배정'),
(20010,1005,10010,'랜덤 배정');

-- ====================== 확인용 쿼리 ======================
SELECT * FROM zones;
SELECT * FROM members;
SELECT * FROM seats;
SELECT * FROM reservations;
SELECT * FROM tickets;
SELECT * FROM reservation_exchanges;
SELECT * FROM auto_assign_log;
DESC members;

-- 같은 존/같은 gno에서 교환가능한 좌석 예시
-- (원본 참고 쿼리 유지)
SELECT r.* FROM reservations r
INNER JOIN seats s ON r.sno = s.sno
WHERE r.gno = (SELECT gno FROM reservations WHERE rno = 40001)
  AND s.zno = (
      SELECT s2.zno FROM reservations r2
      INNER JOIN seats s2 ON r2.sno = s2.sno
      WHERE r2.rno = 40001
  )
  AND r.status = 'reserved'
  AND r.rno != 40001;

-- rno=40004의 구역 전체 좌석
SELECT s.*
FROM seats s
INNER JOIN reservations r
  ON s.zno = (SELECT zno FROM reservations r
              INNER JOIN seats s ON s.sno = r.sno
              WHERE rno = 40004);

-- 동일 쿼리 (좀 더 단순화)
SELECT * FROM seats
WHERE zno = (
  SELECT s.zno FROM reservations r
  INNER JOIN seats s ON s.sno = r.sno
  WHERE r.rno = 40004
);
