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
    email_verified boolean default false not null ,  -- 이메일 인증 여부


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
    seat_no int not null,                     -- 좌석 번호
    senior boolean default false,             -- 시니어 전용 여부
    gno int not null,                         -- 경기 번호 (외부 CSV/크롤링 데이터와 매칭)
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
('user2','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','이순신','010-1111-1112','1990-02-20','user2@test.com','github','gh456',11,false),
('user3','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','강감찬','010-1111-1113','1985-07-15','user3@test.com','facebook','fb789',12,false),
('user4','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','유관순','010-1111-1114','1970-11-05','user4@test.com',null,null,13,false),
('user5','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','안중근','010-1111-1115','2000-03-22','user5@test.com','google','g999',14,false),
('user6','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','윤봉길','010-1111-1116','1962-09-18','user6@test.com',null,null,15,false),
('user7','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','정몽주','010-1111-1117','1995-12-01','user7@test.com','github','gh777',16,false),
('user8','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','신사임당','010-1111-1118','1988-06-25','user8@test.com','facebook','fb888',17,false),
('user9','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','세종대왕','010-1111-1119','1955-08-30','user9@test.com','google','g555',18,false),
('user10','$2a$12$BmTCnFtvOFKtV0DlJDGeYuy4k.WKQtbFENI/uqvwMAFh7paIbI2u2','장영실','010-1111-1120','1999-01-10','user10@test.com',null,null,19,false);


-- 좌석
insert into seats(zno, seat_no, senior , gno) values
(10001,101,false,101),
(10001,102,false,102),
(10002,201,false,103),
(10002,202,false,104),
(10003,301,false,104),
(10004,401,false,105),
(10005,501,false,105),
(10006,601,true,106),
(10007,701,false,106),
(10008,801,false,107);

-- 예매
insert into reservations(mno, sno, status ,gno) values
(20001,30001,'reserved',101),
(20002,30002,'reserved',102),
(20003,30003,'reserved',103),
(20004,30004,'reserved',104),
(20005,30005,'reserved',104),
(20006,30006,'reserved',105),
(20007,30007,'reserved',105),
(20008,30008,'reserved',106),
(20009,30009,'reserved',107),
(20010,30010,'cancelled',108);

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
  r.gno = (select gno from reservations where rno = 40004) and s.zno = (select s2.zno from reservations r2 inner join seats s2 on r2.sno = s2.sno where r2.rno = 40004)
  and r.status = 'reserved' and r.rno != 40004;
