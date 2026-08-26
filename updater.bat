@echo off
chcp 65001 >nul
echo ===================================
echo   Just Visuals Auto Updater
echo ===================================
echo.

set "JAR_NAME=Just-Visuals-1.21.4.jar"
set "VERSION_URL=https://raw.githubusercontent.com/grerr159-bit/just-visuals/main/version.json"
set "MODS_DIR=%APPDATA%\.minecraft\mods"

if not exist "%MODS_DIR%" (
    echo Папка mods не найдена: %MODS_DIR%
    echo Проверь путь к .minecraft
    pause
    exit /b 1
)

echo Проверяю обновление...
powershell -Command "$json = (Invoke-WebRequest -Uri '%VERSION_URL' -UseBasicParsing).Content; $obj = $json | ConvertFrom-Json; Write-Output $obj.version" > "%TEMP%\just_ver.txt"
set /p REMOTE_VER=<"%TEMP%\just_ver.txt"

echo Версия на сервере: %REMOTE_VER%
echo.

echo Скачиваю Just-Visuals v%REMOTE_VER%...
powershell -Command "$json = (Invoke-WebRequest -Uri '%VERSION_URL' -UseBasicParsing).Content; $obj = $json | ConvertFrom-Json; Invoke-WebRequest -Uri $obj.download_url -OutFile '%MODS_DIR%\%JAR_NAME%'" 2>nul

if exist "%MODS_DIR%\%JAR_NAME%" (
    echo.
    echo ===================================
    echo   Готово! Мод обновлён до v%REMOTE_VER%
    echo   Запускай Minecraft!
    echo ===================================
) else (
    echo.
    echo Ошибка при скачивании. Попробуй скачать вручную:
    echo https://github.com/grerr159-bit/just-visuals/releases
)
echo.
pause
