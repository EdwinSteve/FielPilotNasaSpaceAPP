@echo off
rem Doble clic para entrenar el planificador estacional de SURCO (ver README.md).
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0entrenar_windows.ps1" %*
echo.
pause
