@echo off
REM Start Spring Boot app on port 9500
cd /d "d:\all code\tugasakhir"
echo Starting PMB Application on port 9500...
echo.

C:\Users\asus\.jdk\jdk-21.0.8\bin\java.exe -Dserver.port=9500 -Xmx512m -jar target\pmb-system-1.0.0.jar

pause
