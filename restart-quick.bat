@echo off
cd /d "d:\all code\tugasakhir"
echo Killing existing Java processes for port 9500...
REM Use netstat to find and kill, but since taskkill is blocked, use a different method
REM Try using PowerShell to stop processes
powershell -Command "Get-Process java -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*9500*' } | Stop-Process -Force -ErrorAction SilentlyContinue"
timeout /t 2 /nobreak

echo Starting app on port 9500...
start "" /B "C:\Users\asus\.jdk\jdk-21.0.8\bin\java.exe" -Dserver.port=9500 -Xmx512m -jar "target/pmb-system-1.0.0.jar"

timeout /t 3 /nobreak
echo App should be running on http://localhost:9500
