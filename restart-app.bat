@echo off
REM Kill all Java processes
for /f "tokens=2" %%i in ('tasklist ^| find /i "java.exe"') do (
    taskkill /PID %%i /F /T >nul 2>&1
)
timeout /t 3 /nobreak

REM Start app on port 9090
cd /d "d:\all code\tugasakhir"
"C:\Users\asus\.jdk\jdk-21.0.8\bin\java.exe" -Dserver.port=9090 -jar target/pmb-system-1.0.0.jar
