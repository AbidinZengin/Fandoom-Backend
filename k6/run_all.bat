@echo off
REM Tum k6 senaryolarini sirayla calistirir (Windows).
REM Kullanim (cmd):
REM   set BASE_URL=http://localhost:8080
REM   set LOAD_USERNAME=loadtest
REM   set LOAD_PASSWORD=...
REM   k6\run_all.bat
REM (create_thread_test.js icin TOKEN / TOKENS / LOAD_USERS de kullanilabilir)
setlocal
cd /d "%~dp0"

set FAILED=0

echo.
echo === scenarios\feed_load_test.js ===
k6 run scenarios\feed_load_test.js
if errorlevel 1 set FAILED=1

echo.
echo === scenarios\create_thread_test.js ===
k6 run scenarios\create_thread_test.js
if errorlevel 1 set FAILED=1

echo.
if "%FAILED%"=="1" (
  echo En az bir senaryo basarisiz oldu ^(threshold veya hata^).
  exit /b 1
)
echo Tum senaryolar basariyla tamamlandi.
endlocal
