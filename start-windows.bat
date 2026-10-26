@echo off
setlocal
cd /d "%~dp0"
where docker >nul 2>nul
if errorlevel 1 (
 echo Install and start Docker Desktop first. See README.md for the native setup alternative.
 pause
 exit /b 1
)
docker info >nul 2>nul
if errorlevel 1 (
 echo Docker is not ready. Start Docker Desktop, wait for it to finish, then run this file again.
 pause
 exit /b 1
)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup.ps1"
if errorlevel 1 (
 pause
 exit /b 1
)
docker compose up --build -d
if errorlevel 1 (
 echo Build failed. Read the error above. You can inspect services with: docker compose logs
 pause
 exit /b 1
)
echo.
echo Open http://localhost:5173 after the services finish starting.
echo To check startup: docker compose logs backend
echo To stop: docker compose down
pause
