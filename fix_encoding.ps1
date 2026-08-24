# Скрипт для исправления кодировки в Java файлах
# Автоматически находит и исправляет файлы с неправильной кодировкой

Write-Host "Начинаем исправление кодировки..." -ForegroundColor Green

# Счетчики
$totalFiles = 0
$fixedFiles = 0
$errors = 0

# Функция для исправления кодировки
function Fix-FileEncoding {
    param(
        [string]$FilePath
    )
    
    try {
        # Читаем содержимое файла как байты
        $bytes = [System.IO.File]::ReadAllBytes($FilePath)
        
        # Пробуем декодировать как Windows-1251
        $encoding1251 = [System.Text.Encoding]::GetEncoding(1251)
        $content = $encoding1251.GetString($bytes)
        
        # Проверяем, есть ли кракозябры
        if ($content -match '[�?]' -or $content -match '\?\?\?') {
            # Сохраняем в UTF-8
            $utf8 = New-Object System.Text.UTF8Encoding $false
            [System.IO.File]::WriteAllText($FilePath, $content, $utf8)
            return $true
        }
        
        return $false
    }
    catch {
        Write-Host "Ошибка при обработке $FilePath : $_" -ForegroundColor Red
        return $false
    }
}

# Ищем все Java файлы
$javaFiles = Get-ChildItem -Path "src" -Filter "*.java" -Recurse -ErrorAction SilentlyContinue

Write-Host "Найдено файлов: $($javaFiles.Count)" -ForegroundColor Cyan

foreach ($file in $javaFiles) {
    $totalFiles++
    Write-Host "Проверка: $($file.FullName)" -ForegroundColor Gray
    
    if (Fix-FileEncoding -FilePath $file.FullName) {
        $fixedFiles++
        Write-Host "  ✓ Исправлено" -ForegroundColor Green
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Обработка завершена!" -ForegroundColor Green
Write-Host "Всего файлов проверено: $totalFiles" -ForegroundColor White
Write-Host "Исправлено файлов: $fixedFiles" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Cyan

if ($fixedFiles -eq 0) {
    Write-Host "Все файлы уже в правильной кодировке!" -ForegroundColor Yellow
} else {
    Write-Host "Рекомендуется пересобрать проект." -ForegroundColor Yellow
}

# Пауза перед закрытием
Read-Host "Нажмите Enter для выхода"
