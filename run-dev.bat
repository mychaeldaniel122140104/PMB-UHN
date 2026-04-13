@echo off
REM =====================================================
REM PMB System - DEV MODE (dengan auto-reload)
REM =====================================================
REM 
REM 🔧 Fitur:
REM   ✅ Auto-reload HTML/JS/CSS tanpa rebuild
REM   ✅ Auto-reload Java code (dengan restart)
REM   ✅ DevTools enabled
REM   ✅ Template caching disabled
REM   ✅ Live loading dari src/main/resources/static/
REM
REM 🚀 Cara pakai:
REM   Cukup jalankan file ini, lalu edit file di src/main/resources/
REM   Perubahan akan langsung terlihat (refresh browser)
REM
REM 🎯 Keyboard shortcuts saat running:
REM   Ctrl+C = Stop server
REM
REM =====================================================

echo.
echo ================================================
echo 🔥  PMB SYSTEM - DEV MODE (Auto-Reload)
echo ================================================
echo.
echo 📝 Konfigurasi:
echo   • Port: 9500
echo   • DevTools: ENABLED
echo   • Auto-reload HTML/JS: YA
echo   • Auto-reload Java: YA (dengan restart)
echo.
echo 💡 Tips:
echo   1. Edit file di: src/main/resources/
echo   2. Refresh browser untuk lihat perubahan
echo   3. Tekan Ctrl+Shift+R untuk clear cache
echo.
echo ⏳ Starting server...
echo (Ini bisa butuh 10-15 detik untuk startup)
echo.

cd /d "%~dp0"
call mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9500"

echo.
echo ❌ Server stopped.
pause
