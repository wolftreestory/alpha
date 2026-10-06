DROP TABLE IF EXISTS TB_BOARD;

CREATE TABLE TB_BOARD (
  IDX INT AUTO_INCREMENT PRIMARY KEY
  ,TITLE VARCHAR(250) NOT NULL
  ,CONTENT VARCHAR(1000) NOT NULL
  ,REG_NAME VARCHAR(250) NOT NULL
  ,REG_DATE DATE NOT NULL
  ,MOD_DATE DATE NULL
);

INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha 배치(스케쥴러) 시스템-01','[A]심수봉 백만송이 장미 / https://www.youtube.com/watch?v=MPCD3dIwfdI','심수봉',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha 배치(스케쥴러) 시스템-02','[B]Dovè LAmore(사랑은 어디에) / https://www.youtube.com/watch?v=WW-k-iKVRy4','Cher',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha 배치(스케쥴러) 시스템-03','[C]恋人よ(연인이여) / https://www.youtube.com/watch?v=o54sl6lWnwI','이츠와마유미',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha 배치(스케쥴러) 시스템-04','[D]Crazy / https://www.youtube.com/watch?v=x8j2UFW3IAY','Gnarls Barkley',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha 배치(스케쥴러) 시스템-05','[E]Amado Mio(나의 사랑) / https://www.youtube.com/watch?v=sCbzWiJLVhk','Pink Martini ft. Storm Large',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha 배치(스케쥴러) 시스템-06','[F]Beggin’ / https://www.youtube.com/watch?v=Xg72z08aTXY','Måneskin',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha 배치(스케쥴러) 시스템-07','[G]Bella ciao(Goodbye Beautiful) / https://www.youtube.com/watch?v=KLGY_htXtPI','Goran Bregovic',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha 배치(스케쥴러) 시스템-08','[H]Histoire d''un Amour(사랑 이야기) / https://www.youtube.com/watch?v=YsovX4Beas0','Chico & Gypsies',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha 배치(스케쥴러) 시스템-09','[I]Nowhere Fast / https://www.youtube.com/watch?v=arxD3Ro9mAk','Streets of Fire',now());
INSERT INTO TB_BOARD (TITLE,CONTENT,REG_NAME,REG_DATE) VALUES ('alpha 배치(스케쥴러) 시스템-10','[J]Les Feuilles Mortes (Fallen Leaves) / https://www.youtube.com/watch?v=P85xNNTrQEw','고우림',now());