# Java 8 JRE official image; supports linux/arm64 used by the Ubuntu VM.
FROM support.apollo.co.kr:7060/library/eclipse-temurin:8-jre-jammy

ENV TZ=Asia/Seoul \
    PROFILE=dev

WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
COPY entrypoint.sh run.sh
RUN chmod 755 run.sh

EXPOSE 9970

ENTRYPOINT ["sh", "./run.sh"]
