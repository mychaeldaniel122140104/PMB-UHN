@echo off
setlocal enabledelayedexpansion

cd /d "d:\all code\tugasakhir"

echo Killing Java processes...
for /f "tokens=5" %%A in ('tasklist ^| find /i "java.exe"') do (
    taskkill /PID %%A /F /T 2>nul
)

echo Waiting 3 seconds...
timeout /t 3 /nobreak

echo Cleaning target directory...
rmdir /s /q target 2>nul

echo Building application...
set JAVA_HOME=C:\Users\asus\.jdk\jdk-21.0.8
call C:\Users\asus\.maven\maven-3.9.12\bin\mvn.cmd clean package -DskipTests -q -Dorg.slf4j.simpleLogger.logFile=build.log

echo Starting application on port 9090...
start cmd /k "cd /d d:\all code\tugasakhir && "C:\Users\asus\.jdk\jdk-21.0.8\bin\java.exe" -Dserver.port=9090 -jar target/pmb-system-1.0.0.jar"

echo Application starting... (window will open separately)
echo Access at: http://localhost:9090/login.html
echo Admin: admin@pmb.com / admin123
timeout /t 5

endlocal
