@echo off
REM ===== DEVELOPMENT MODE STARTUP SCRIPT =====
REM Ini adalah cara yang BENAR untuk menjalankan Spring Boot di development mode
REM dengan auto-reload untuk HTML/JS/CSS

echo.
echo ========================================
echo 🔥 DEV MODE WITH AUTO-RELOAD ENABLED 🔥
echo ========================================
echo.
echo ✅ Server akan serve dari: src/main/resources/static/
echo ✅ Edit file → Save → Refresh browser = Langsung terlihat
echo ✅ DevTools enabled = Auto reload jalan
echo.
echo ⚙️  Building project dengan DevTools...
echo.

REM First time setup - clean build
call mvn clean install

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ Maven build failed!
    pause
    exit /b 1
)

echo.
echo ✅ Build SUCCESS! Starting development server...
echo.
echo 🚀 Starting: mvn spring-boot:run
echo.
echo Repository telah dikonfigurasi:
echo   - Static location: file:src/main/resources/static/
echo   - Cache disabled: true
echo   - DevTools restart: enabled
echo   - Thymeleaf cache: false
echo.
echo 📝 Tips:
echo   1. Edit file di: src/main/resources/static/
echo   2. Save file (Ctrl+S)
echo   3. Refresh browser (Ctrl+Shift+R atau F5)
echo   4. Perubahan langsung terlihat - JANGAN RESTART LAGI
echo.
echo ⏹️  Untuk berhenti: Tekan Ctrl+C
echo.

REM Run dengan spring-boot:run (NOT java -jar)
mvn spring-boot:run

pause
