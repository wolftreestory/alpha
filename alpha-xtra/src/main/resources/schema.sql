DROP TABLE IF EXISTS TB_BOARD;

CREATE TABLE TB_BOARD (
  IDX INT AUTO_INCREMENT PRIMARY KEY
  ,TITLE VARCHAR(250) NOT NULL
  ,CONTENT VARCHAR(1000) NOT NULL
  ,REG_NAME VARCHAR(250) NOT NULL
  ,REG_DATE DATE NOT NULL
  ,MOD_DATE DATE NULL
);

INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha-01','[A]심수봉 백만송이 장미 / https://www.youtube.com/watch?v=MPCD3dIwfdI','심수봉',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha-02','[B]Dovè LAmore(사랑은 어디에) / https://www.youtube.com/watch?v=WW-k-iKVRy4','Cher',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha-03','[C]恋人よ(연인이여) / https://www.youtube.com/watch?v=o54sl6lWnwI','이츠와마유미',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha-04','[D]Crazy / https://www.youtube.com/watch?v=x8j2UFW3IAY','Gnarls Barkley',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha-05','[E]Amado Mio(나의 사랑) / https://www.youtube.com/watch?v=sCbzWiJLVhk','Pink Martini ft. Storm Large',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha-06','[F]Beggin’ / https://www.youtube.com/watch?v=Xg72z08aTXY','Måneskin',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha-07','[G]Bella ciao(Goodbye Beautiful) / https://www.youtube.com/watch?v=KLGY_htXtPI','Goran Bregovic',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha-08','[H]Histoire d''un Amour(사랑 이야기) / https://www.youtube.com/watch?v=YsovX4Beas0','Chico & Gypsies',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha-09','[I]Nowhere Fast / https://www.youtube.com/watch?v=arxD3Ro9mAk','Streets of Fire',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha-10','[J]Les Feuilles Mortes (Fallen Leaves) / https://www.youtube.com/watch?v=P85xNNTrQEw','고우림',now());



/*
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('테스트 데이터01(dữ liệu thử nghiệm01)(Test Data01)','[A]안녕하세요(Xin chào)(hello)','Kim Jaemin',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('테스트 데이터02(dữ liệu thử nghiệm02)(Test Data02)','[B]감사합니다(Cám ơn)(thank You)','Yoon Jeonnam ',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('테스트 데이터03(dữ liệu thử nghiệm03)(Test Data03)','[C]지구(trái đất)(earth)','Kim Yongwoon',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('테스트 데이터04(dữ liệu thử nghiệm04)(Test Data04)','[D]달(mặt trăng)(moon)','Min Sangho',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('테스트 데이터05(dữ liệu thử nghiệm05)(Test Data05)','[E]엄마(mẹ)(mother)','Kang Changik',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('테스트 데이터06(dữ liệu thử nghiệm06)(Test Data06)','[F]아빠(bố)(father)','Park Hana',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('테스트 데이터07(dữ liệu thử nghiệm07)(Test Data07)','[G]학교(trường học)(school)','Choi Bowon',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('테스트 데이터08(dữ liệu thử nghiệm08)(Test Data08)','[H]회사(công ty)(company)','Han Heeju',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('테스트 데이터09(dữ liệu thử nghiệm09)(Test Data09)','[I]음악(âm nhạc)(music)','Hong Gildong',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('테스트 데이터10(dữ liệu thử nghiệm10)(Test Data10)','[J]과학(khoa học)(science)','Ma Dongseok',now());
*/