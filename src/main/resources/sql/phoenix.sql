drop database if exists phoenix;
create database phoenix;
use phoenix;

-- ---------------------- 구역 테이블 ----------------------
create table zones(
    zno int auto_increment primary key,        -- 구역 고유번호(PK)
    zname varchar(50) not null,                -- 구역 이름
    price int not null                         -- 해당 구역 가격    
);

-- ---------------------- 회원 테이블 ---------------------- 
create table members (
    mno int auto_increment primary key,            -- 회원 고유번호(PK)
    mid varchar(50) unique null,               -- 회원 아이디
    password_hash varchar(255) null,           -- 비밀번호 해시값
    mname varchar(50) not null,                    -- 회원 이름
    mphone varchar(13) unique null,            -- 회원 번호
    birthdate date not null,                       -- 생년월일
    email varchar(100) unique not null,            -- 이메일
    create_at timestamp default current_timestamp, -- 가입일(로그 추적용)

    -- 소셜 로그인 연동용
    provider varchar(50),        -- 소셜 제공자 (google, github, facebook 등)
    provider_id varchar(100),    -- 소셜 제공자 내부 고유 ID

    -- 선호 선수 (FK 제거, CSV/크롤링 데이터와 매칭)
    pno int null,

    -- 회원 상태 관리
    status enum('active','dormant','withdrawn') default 'active' not null ,
    -- active: 정상회원 / dormant: 휴면회원 / withdrawn: 탈퇴회원

    -- 교환 신청 가능 여부
    exchange boolean default true not null,
    -- true: 교환 신청 가능 / false: 교환 신청 불가
    
    -- 이메일 인증 여부( 이메일 인증 완료 여부 저장 )
    email_verified boolean default false not null   -- 이메일 인증 여부
    
    
);

-- 소셜 회원 unique 유지하면서 null 허용
ALTER TABLE members MODIFY COLUMN mphone VARCHAR(13) NULL UNIQUE;

-- ---------------------- 시니어 회원 뷰 ---------------------- 
create or replace view member_view as
select m.*, 
       (timestampdiff(year, m.birthdate, curdate()) >= 65) as is_senior
from members m;

-- ---------------------- 좌석 테이블 ----------------------
create table seats(
    sno int auto_increment primary key,       -- 좌석 고유번호(PK)
    zno int not null,                         -- 구역 번호
    seatName varchar(30) not null,            -- 좌석 이름
    senior boolean default false,             -- 시니어 전용 여부
    foreign key(zno) references zones(zno)
);

-- ---------------------- 예매 테이블 ----------------------
-- FK 유지: 회원, 좌석은 반드시 존재해야 함
-- gno는 CSV/크롤링 기반으로만 관리 → FK 제거
create table reservations(
    rno int auto_increment primary key,       -- 예매 고유번호(PK)
    mno int not null,                         -- 회원 번호
    sno int not null,                         -- 좌석 번호
    gno int not null,                         -- 경기 번호 (외부 CSV/크롤링 데이터와 매칭)
    reserved_at timestamp default current_timestamp,
    status enum('reserved','cancelled') default 'reserved',
    foreign key(mno) references members(mno),
    foreign key(sno) references seats(sno)
);

-- ---------------------- 티켓 테이블 ----------------------
-- FK 유지: 예매와 반드시 연결
create table tickets (
    tno int auto_increment primary key,       -- 티켓 고유번호(PK)
    rno int not null,                         -- 관련된 예매(FK)
    ticket_code varchar(100) unique not null, -- 티켓 코드(UUID/QR 등)
    issued_at timestamp default current_timestamp,
    valid boolean default true,               -- 유효 여부
    price int not null,                       -- 티켓 발급 시 가격
    foreign key(rno) references reservations(rno)
);

-- ---------------------- 예매 교환 테이블 ----------------------
-- FK 유지: 교환 대상은 반드시 실제 존재하는 예매여야 함
create table reservation_exchanges(
    exno int auto_increment primary key,
    from_rno int not null,                    -- 요청 예매 번호
    to_rno int not null,                      -- 대상 예매 번호
    status enum('pending','approved','rejected') default 'pending',
    requested_at timestamp default current_timestamp,
    responded_at timestamp null,
    foreign key(from_rno) references reservations(rno),
    foreign key(to_rno) references reservations(rno)
);

-- ---------------------- 자동배정 로그 테이블 ----------------------
-- FK 제거: 로그성 데이터 (존재하지 않는 값도 기록 가능해야 함)
create table auto_assign_log(
    lno int auto_increment primary key,
    mno int not null,                         -- 회원 번호 (존재 검증은 서비스단에서)
    gno int not null,                         -- 경기 번호 (CSV/크롤링 기반)
    assigned_zno int,                         -- 배정 구역 번호
    reason varchar(255),
    create_at timestamp default current_timestamp
);

-- ---------------------- 고유 번호 제약 ----------------------
alter table zones auto_increment=10001;
alter table members auto_increment = 20001;
alter table seats auto_increment = 30001;
alter table reservations auto_increment = 40001;
alter table tickets auto_increment = 50001;
alter table reservation_exchanges auto_increment = 60001;
alter table auto_assign_log auto_increment = 70001;

-- ---------------------- 샘플 ----------------------
-- 구역
insert into zones(zname, price) values
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
insert into members (
    mid, password_hash, mname, mphone, birthdate, email,
    provider, provider_id, pno,
    email_verified
) values
('user1','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','홍길동','010-1111-1111','1960-05-10','user1@test.com','google','g123',10,true),
('user2','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','이순신','010-1111-1112','1990-02-20','user2@test.com','github','gh456',11,true),
('user3','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','강감찬','010-1111-1113','1985-07-15','user3@test.com','facebook','fb789',12,true),
('user4','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','유관순','010-1111-1114','1970-11-05','user4@test.com',null,null,13,true),
('user5','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','안중근','010-1111-1115','2000-03-22','user5@test.com','google','g999',14,true),
('user6','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','윤봉길','010-1111-1116','1962-09-18','user6@test.com',null,null,15,true),
('user7','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','정몽주','010-1111-1117','1995-12-01','user7@test.com','github','gh777',16,true),
('user8','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','신사임당','010-1111-1118','1988-06-25','user8@test.com','facebook','fb888',17,true),
('user9','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','세종대왕','010-1111-1119','1955-08-30','user9@test.com','google','g555',18,true),
('user10','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','장영실','010-1111-1120','1999-01-10','user10@test.com',null,null,19,true);


-- 좌석
-- 겨레석 (zno = 10001)
insert into seats(zno, seatName, senior) values
(10001,'A1',true),(10001,'A2',true),(10001,'A3',true),(10001,'A4',true),(10001,'A5',true),
(10001,'A6',true),(10001,'A7',true),(10001,'A8',true),(10001,'A9',true),(10001,'A10',true),
(10001,'B1',false),(10001,'B2',false),(10001,'B3',false),(10001,'B4',false),(10001,'B5',false),
(10001,'B6',false),(10001,'B7',false),(10001,'B8',false),(10001,'B9',false),(10001,'B10',false),
(10001,'C1',false),(10001,'C2',false),(10001,'C3',false),(10001,'C4',false),(10001,'C5',false),
(10001,'C6',false),(10001,'C7',false),(10001,'C8',false),(10001,'C9',false),(10001,'C10',false);

-- 연우석 (zno = 10002)
insert into seats(zno, seatName, senior) values
(10002,'A1',true),(10002,'A2',true),(10002,'A3',true),(10002,'A4',true),(10002,'A5',true),
(10002,'A6',true),(10002,'A7',true),(10002,'A8',true),(10002,'A9',true),(10002,'A10',true),
(10002,'B1',false),(10002,'B2',false),(10002,'B3',false),(10002,'B4',false),(10002,'B5',false),
(10002,'B6',false),(10002,'B7',false),(10002,'B8',false),(10002,'B9',false),(10002,'B10',false),
(10002,'C1',false),(10002,'C2',false),(10002,'C3',false),(10002,'C4',false),(10002,'C5',false),
(10002,'C6',false),(10002,'C7',false),(10002,'C8',false),(10002,'C9',false),(10002,'C10',false);

-- 성호석 (zno = 10003)
insert into seats(zno, seatName, senior) values
(10003,'A1',true),(10003,'A2',true),(10003,'A3',true),(10003,'A4',true),(10003,'A5',true),
(10003,'A6',true),(10003,'A7',true),(10003,'A8',true),(10003,'A9',true),(10003,'A10',true),
(10003,'B1',false),(10003,'B2',false),(10003,'B3',false),(10003,'B4',false),(10003,'B5',false),
(10003,'B6',false),(10003,'B7',false),(10003,'B8',false),(10003,'B9',false),(10003,'B10',false),
(10003,'C1',false),(10003,'C2',false),(10003,'C3',false),(10003,'C4',false),(10003,'C5',false),
(10003,'C6',false),(10003,'C7',false),(10003,'C8',false),(10003,'C9',false),(10003,'C10',false);

-- 찬영석 (zno = 10004)
insert into seats(zno, seatName, senior) values
(10004,'A1',true),(10004,'A2',true),(10004,'A3',true),(10004,'A4',true),(10004,'A5',true),
(10004,'A6',true),(10004,'A7',true),(10004,'A8',true),(10004,'A9',true),(10004,'A10',true),
(10004,'B1',false),(10004,'B2',false),(10004,'B3',false),(10004,'B4',false),(10004,'B5',false),
(10004,'B6',false),(10004,'B7',false),(10004,'B8',false),(10004,'B9',false),(10004,'B10',false),
(10004,'C1',false),(10004,'C2',false),(10004,'C3',false),(10004,'C4',false),(10004,'C5',false),
(10004,'C6',false),(10004,'C7',false),(10004,'C8',false),(10004,'C9',false),(10004,'C10',false);

-- 중앙테이블석 (zno = 10005)
insert into seats(zno, seatName, senior) values
(10005,'A1',true),(10005,'A2',true),(10005,'A3',true),(10005,'A4',true),(10005,'A5',true),
(10005,'A6',true),(10005,'A7',true),(10005,'A8',true),(10005,'A9',true),(10005,'A10',true),
(10005,'B1',false),(10005,'B2',false),(10005,'B3',false),(10005,'B4',false),(10005,'B5',false),
(10005,'B6',false),(10005,'B7',false),(10005,'B8',false),(10005,'B9',false),(10005,'B10',false),
(10005,'C1',false),(10005,'C2',false),(10005,'C3',false),(10005,'C4',false),(10005,'C5',false),
(10005,'C6',false),(10005,'C7',false),(10005,'C8',false),(10005,'C9',false),(10005,'C10',false);

-- 외야자유석 (zno = 10006)
insert into seats(zno, seatName, senior) values
(10006,'A1',true),(10006,'A2',true),(10006,'A3',true),(10006,'A4',true),(10006,'A5',true),
(10006,'A6',true),(10006,'A7',true),(10006,'A8',true),(10006,'A9',true),(10006,'A10',true),
(10006,'B1',false),(10006,'B2',false),(10006,'B3',false),(10006,'B4',false),(10006,'B5',false),
(10006,'B6',false),(10006,'B7',false),(10006,'B8',false),(10006,'B9',false),(10006,'B10',false),
(10006,'C1',false),(10006,'C2',false),(10006,'C3',false),(10006,'C4',false),(10006,'C5',false),
(10006,'C6',false),(10006,'C7',false),(10006,'C8',false),(10006,'C9',false),(10006,'C10',false);

-- 예매
insert into reservations (mno, sno, gno, status) values
-- 회원 20001 (3건, 1건 취소)
(20001, 30001, 1, 'reserved'),     -- 겨레석 A1
(20001, 30002, 1, 'cancelled'),    -- 겨레석 A2 (취소됨)
(20001, 30003, 1, 'reserved'),     -- 겨레석 A3
(20001, 30003,146,'reserved'),
-- 회원 20002 (3건, 1건 취소)
(20002, 30031, 2, 'reserved'),     -- 연우석 A1
(20002, 30032, 2, 'reserved'),     -- 연우석 A2
(20002, 30033, 2, 'cancelled'),    -- 연우석 A3 (취소됨)
(20002, 30004,146,'reserved'),

-- 회원 20003 (3건, 1건 취소)
(20003, 30061, 3, 'reserved'),     -- 성호석 A1
(20003, 30062, 3, 'cancelled'),    -- 성호석 A2 (취소됨)
(20003, 30063, 3, 'reserved'),     -- 성호석 A3

-- 회원 20004 (1건)
(20004, 30091, 4, 'reserved');     -- 찬영석 A1

-- 티켓
insert into tickets(rno, ticket_code, valid, price) values
(40001,'TCKT001',true,30000),
(40002,'TCKT002',true,30000),
(40003,'TCKT003',true,20000),
(40004,'TCKT004',true,20000),
(40005,'TCKT005',true,50000),
(40006,'TCKT006',true,50000),
(40007,'TCKT007',true,80000),
(40008,'TCKT008',true,15000),
(40009,'TCKT009',true,120000),
(40010,'TCKT010',false,35000);

-- 예매 교환
insert into reservation_exchanges(from_rno, to_rno, status) values
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

-- 자동배정 로그
insert into auto_assign_log(mno, gno, assigned_zno, reason) values
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


-- ---------------------- 샘플 조회 ----------------------
select * from zones;
select * from members;
select * from seats;
select * from reservations;
select * from tickets;
select * from reservation_exchanges;
select * from auto_assign_log;
DESC members;

# 교환가능한 좌석정보
select r.* from reservations r inner join seats s on r.sno = s.sno where
  r.gno = (select gno from reservations where rno = 40001) and s.zno = (select s2.zno from reservations r2 inner join seats s2 on r2.sno = s2.sno where r2.rno = 40001)
  and r.status = 'reserved' and r.rno != 40001;
  select s.* from seats s inner join reservations r on s.zno = (select zno from reservations r inner join seats s on s.sno = r.sno where rno = 40004);
  SELECT * FROM seats WHERE zno = (SELECT s.zno FROM reservations r INNER JOIN seats s ON s.sno = r.sno WHERE r.rno = 40004);