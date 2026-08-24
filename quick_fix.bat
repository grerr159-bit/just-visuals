@echo off
chcp 65001 >nul
echo ╔════════════════════════════════════════════════════════╗
echo ║        Быстрое исправление кодировки                  ║
echo ╚════════════════════════════════════════════════════════╝
echo.
echo Запуск скрипта исправления...
echo.

powershell.exe -ExecutionPolicy Bypass -File "%~dp0fix_all_encoding.ps1"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✓ Скрипт выполнен успешно!
) else (
    echo.
    echo ✗ Произошла ошибка при выполнении скрипта
)

pause
