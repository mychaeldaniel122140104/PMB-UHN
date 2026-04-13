@echo off
REM Kill all Java processes
for /f "tokens=2" %%i in ('tasklist ^| find /i "java.exe"') do (
    taskkill /PID %%i /F /T >nul 2>&1
)
timeout /t 3 /nobreak

REM Start app with Maven spring-boot:run
cd /d "d:\all code\tugasakhir"
set JAVA_HOME=C:\Users\asus\.jdk\jdk-21.0.8
C:\Users\asus\.maven\maven-3.9.12\bin\mvn.cmd spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"
