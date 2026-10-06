###############################################################
# Docker 이미지(image)가 어떤 단계를 거쳐 빌드(build)되야 하는지를 담고있는 텍스트 파일입니다. 
#	1.베이스 이미지 선택: 이미지를 구성할 때 기존 이미지를 기반으로 시작하는데, 이 이미지를 '베이스 이미지'라고 한다.
#	2.필요한 파일 및 명령 추가: 베이스 이미지 위에 필요한 파일과 명령을 추가. (파일 복사, 환경 변수 설정, 응용프로그램 설치)
#	3.작업 디렉토리 설정: 이미지 내에서 작업 디렉토리를 설정하여 파일을 추가하거나 명령을 실행하는 위치를 지정.
#	4.명령 실행: 이미지 내에서 실행할 명령을 정의. RUN, CMD, ENTRYPOINT 등의 명령을 사용하여 작업을 수행.
#	5.포트 노출: 이미지가 실행될 때 개방할 포트를 지정. EXPOSE 명령을 사용하여 컨테이너 내부에서 개방할 포트를 명시.
#	6.이미지 메타데이터 설정: 이미지의 메타데이터를 설정할 수 있음
#
###############################################################
# Docker는 Dockerfile에 나열된 명령문을 차례대로 수행하여 이미지를 생성
#	FROM : base 이미지 설정
#	WORKDIR : 작업 디렉터리 설정
#	ARG : 빌드 시 넘어올 수 있는 인자 설정
#	ENV : 환경 변수 설정
#	COPY : 호스트 파일 시스템에서 파일이나 디렉토리를 이미지 내부로 복사
#	ADD : COPY와 유사하지만 추가 기능을 지원. URL로부터 파일을 다운로드하거나 압축 파일을 풀 수 있음
#	RUN : 컨테이너 내에서 실행할 명령어 정의. 패키지 설치, 파일 복사, 응용프로그램 빌드 등
#	EXPOSE : 컨테이너가 개방할 포트 지정
#	CMD : 컨테이너가 실행될 때 실행할 명령어 지정. 'docker run'  명령에서 명령어가 제공되지 않을 경우에만 실행
#	ENTRYPOINT : 이미지 실행 시 항상 실행되야 하는 커맨드 설정
#	VOLUME : 호스트와 컨테이너 간 볼륨을 공유
#
###############################################################
# Dockerfile 빌드 : docker build [옵션] <경로>
#	-t(--tag) <이미지명:태그>	: 빌드된 이미지에 이름과 태그를 지정, 일반적으로 이미지명:태그 형식으로 사용
#	-f(--file) <파일경로>	: 빌드에 사용할 Dockerfile의 경로를 지정, 기본적으로 현재 디렉토리의 Dockerfile을 사용하지만, 다른 경로의 Dockerfile을 사용하려면 이 옵션을 사용
#	--build-arg <변수명=값>	: Dockerfile 내에서 사용되는 빌드 인수(argument)를 지정, Dockerfile 내에서 ARG로 정의된 인수에 값을 전달할 때 사용
#	--no-cache			: 이전 빌드 캐시를 사용하지 않고 항상 새로운 이미지를 빌드
#	--network <네트워크>	: 빌드 중에 사용할 Docker 네트워크를 지정합니다. 다른 컨테이너와의 통신이 필요한 경우 사용
#	--pull				: 이미지를 빌드하기 전에 Docker Hub나 레지스트리에서 최신 이미지를 가져옴
#	--target <스테이지이름>	: 멀티스테이지 빌드에서 특정 스테이지를 빌드합니다. Dockerfile 내에서 각 스테이지를 정의해야 함
#	
#	Dockerfile 위치 지정	: docker build -t my-image:latest -f /path/to/Dockerfile .
#	빌드 컨텍스트 변경		: docker build -t my-image:latest -f /path/to/Dockerfile /path/to/build/context
#	빌드 태그 설정			: docker build -t my-image:v1.0 .
#	빌드 캐시 사용 제외		: docker build --no-cache -t my-image:latest .
#	ARG 값 전달			: docker build --build-arg MY_ARG=value -t my-image:latest .
#	빌드 중간 단계 삭제		: docker build --target=my-stage -t my-image:latest .
#
#	(ex) docker build --network host -t alpha-xtra:v1.0 .
###############################################################
# Multi Stage
#	이미지 크기 줄이기 : 가장 많이 활용하는 사례는 도커이미지 줄이기입니다. 모든 도커 이미지를 줄일 수 없고 빌드 단계가 있는 도커 이미지 등 단계가 분리되어 있는 Dockerfile만 적용가능.
#	빌드속도향상 : Multi Stage를 사용하면 Dockerfile작업을 병렬로 실행하여 빌드 속도를 향상
#	Multi Stage는 여러 개의 BaseImage를 사용하여 docker build를 수행합니다. 
#	BaseImage는 Docker build에 사용하는 기본이미지입니다. Dockerfile에서 FROM에 설정된 이미지에 해당합니다.
#	FROM키워드를 기준으로 작업공간이 분리되는데, 분리된 작업공간을 stage라고합니다. 
#	Multi Stage는 stage가 2개 이상일 때 해당합니다.
#
###############################################################
# docker run : docker run [OPTIONS] IMAGE[:TAG|@DIGEST] [COMMAND] [ARG...]
#	docker run은 docker image로 부터 만든 새로운 컨테이너에서 명령어를 실행 시키는 명령어
#	-i			: 컨테이너에 attach(접속)하지 않은 상태여도 표준 입력(stdin)을 활성화
#	-t			: TTY(Teletypewriter) 모드(pseudo-TTY)를 사용하는 것으로, 쉘에 명령어를 작성
#	-it			: docker는 pseudo-TTY를 할당하고, container의 stdin에 연결
#	-d			: 컨테이너를 백그라운드에서 실행하도록 하는 옵션
#	-e			: 컨테이너에 환경 변수를 추가하는 옵션, 여러개를 추가하고 싶다면 -e를 여러번 사용
#	-p			: 호스트에 연결된 컨테이너의 특정 포트를 외부에 노출
#	-w			: working directory를 변경하는 옵션
#	-v			: working directory를 컨테이너에 마운트 하는 옵션이다 ( -t <호스트 디렉터리>:<컨테이너 디렉터리> )
#	-u			: user 또는 uid로 컨테이너에 접속하기 위해 사용하는 옵션
#	-h			: 컨테이너의 호스트 네임을 설정하는 옵션
#	--name		: 컨테이너의 이름을 설정할 때 사용하는 옵션
#	--rm			: 명령어 수행 후 container가 삭제 되도록 하는 옵션 (컨테이너를 일회성으로 사용할 때 주로 사용)
#
###############################################################
# 명령어
# docker --version : 설치되었는지 확인 (sudo systemctl status docker : 도커 실행)
# docker image bulid -t 이미지명[:태그명] Dockerfile경로 : 도커 이미지 빌드
# docker image pull 레파지토리명[:태그명] : 도커 이미지 pull
# docker images : 이미지 목록
# docker images -a : 전체 이미지 목록
# docker images -aq : 전체 이미지ID
# docker ps : 실행중인 컨테이너 목록 확인 (
# docker ps -a : 전체 컨테이너 목록 확인
# docker ps -aq : 전체 컨테이너ID
# docker container ls -a : 전체 컨테이너 목록 확인
# docker start 컨테이너ID : 컨테이너 시작
# docker attach 컨테이너ID : 컨테이너 접속
# docker stop 컨테이너ID : 컨테이너 멈춤
# docker run 컨테이너ID : 컨테이너 생성 및 시작
# docker run -it 컨테이너ID : 컨테이너 생성 및 시작 및 접속
# docker rm 컨테이너ID : 컨테이너 삭제
# docker exec -it 컨테이너ID /bin/bash : 실행되고 있던 컨테이너 접속
# exit : 컨테이너 빠져나오기
#
###############################################################
# 빌드명령어 : docker build --network host -t alpha-xtra:v1.0 .
# 실행명령어 : docker run -d -p 9970:9970 alpha-xtra:v1.0
# 컨테이너접속 : docker exec -it  [컨테이너ID] /bin/sh
#
###############################################################

FROM support.apollo.co.kr:7060/openjdk:8-jdk-alpine
RUN apk --no-cache add tzdata && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime
ARG JAR_FILE=target/*.jar
WORKDIR /app
COPY ${JAR_FILE} app.jar
COPY entrypoint.sh run.sh
RUN chmod 774 run.sh
ENV PROFILE dev
EXPOSE 9970
ENTRYPOINT ["sh","./run.sh"]