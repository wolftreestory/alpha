@echo off
SETLOCAL

SET JAVA_HOME=C:\azure.ide\support\java\openjdk-8u422-b05
SET MAVEN_HOME=C:\azure.ide\support\maven\apache-maven-3.9.9
SET PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%

SET MVN_OPTS=-s C:\azure.ide\support\maven\settings-nexus.xml -U clean package -Dfile.encoding=UTF-8

cmd /c mvn %MVN_OPTS% -f C:\azure.ide\workspace\alpha-batch-pack1\pom.xml 

cmd /c mvn %MVN_OPTS% -f C:\azure.ide\workspace\alpha-batch-pack2\pom.xml