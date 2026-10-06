#! /bin/sh

ACTIVE_PROFILE="${PROFILE:-dev}"
LOG_BASE=/app/log

#echo "ACTIVE_PROFILE=${ACTIVE_PROFILE}"
#echo "LOG_BASE=${LOG_BASE}"
#exec java -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=${ACTIVE_PROFILE} -jar app.jar

_appJar="app.jar"
_appOptions=" --spring.profiles.active=${ACTIVE_PROFILE}"
_consoleLog="/dev/null"
_gcLogPath=$LOG_BASE/gc
_heapDumpPath=$LOG_BASE"/dump"

mkdir -p $_gcLogPath
mkdir -p $_heapDumpPath

_jvmOpts=""
_jvmOpts=$_jvmOpts" -server"                                    
_jvmOpts=$_jvmOpts" -Xms256m -Xmx512m"
_jvmOpts=$_jvmOpts" -verbose:gc" #GC가 수행될때 로깅을 남기는 옵션
_jvmOpts=$_jvmOpts" -Xloggc:$_gcLogPath/gc.log.$(date +%Y%m%d)" #GC 로그를 파일
_jvmOpts=$_jvmOpts" -XX:+PrintGCDetails" #GC 수행시에 더 자세한 정보를 출력
_jvmOpts=$_jvmOpts" -XX:+PrintGCTimeStamps" #GC 이벤트 발생 시간을 (VM 시작 이후 경과한 시간을 초 단위로) 출력
_jvmOpts=$_jvmOpts" -XX:+PrintGCDateStamps" #GC 이벤트 발생 시간을 (벽시계 시간 기준으로) 출력
_jvmOpts=$_jvmOpts" -XX:+UseConcMarkSweepGC" #Concurrent Mark And Sweep 방식의 GC를 사용
_jvmOpts=$_jvmOpts" -XX:+UseParNewGC" #Young generation에서 발생하는 GC 방식을 multi 스레드 방식으로 진행
_jvmOpts=$_jvmOpts" -XX:+CMSParallelRemarkEnabled" #Young generation 에서 발생하는 Remark 단계를 병렬로 진행
_jvmOpts=$_jvmOpts" -XX:+DisableExplicitGC" #System.gc() 메소드를 호출해도 GC가 수행되지 않음
_jvmOpts=$_jvmOpts" -XX:+HeapDumpOnOutOfMemoryError" #OutOfMemoryError 오류가 발생시 heap dump를 생성
_jvmOpts=$_jvmOpts" -XX:HeapDumpPath=$_heapDumpPath"

#nohup java $_jvmOpts -jar $_appJar $_appOptions 1>$_consoleLog 2>&1 &
exec java $_jvmOpts -jar $_appJar $_appOptions