@echo off
cd /d "d:\all code\tugasakhir"
"C:\Users\asus\.jdk\jdk-21.0.8\bin\java.exe" -Dserver.port=9500 -Xmx512m -jar "target/pmb-system-1.0.0.jar"
pause
