# Универсальный скрипт для исправления кодировки во всех Java файлах
# Поддерживает различные типы кодировок и автоматическое определение

Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   Скрипт исправления кодировки Java файлов v2.0       ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

$totalFiles = 0
$fixedFiles = 0
$skippedFiles = 0

# Список кодировок для проверки
$encodings = @(
    [System.Text.Encoding]::GetEncoding(1251),  # Windows-1251 (Cyrillic)
    [System.Text.Encoding]::GetEncoding(866),   # CP866 (DOS Cyrillic)
    [System.Text.Encoding]::GetEncoding("ISO-8859-5")  # ISO Cyrillic
)

function Test-HasGarbledText {
    param([string]$Content)
    
    # Проверяем наличие кракозябр
    if ($Content -match '[�]') { return $true }
    if ($Content -match '\?\?\?') { return $true }
    if ($Content -match '[\x00-\x08\x0B\x0C\x0E-\x1F]') { return $true }
    
    return $false
}

function Fix-FileEncoding {
    param([string]$FilePath)
    
    try {
        $bytes = [System.IO.File]::ReadAllBytes($FilePath)
        $originalContent = [System.IO.File]::ReadAllText($FilePath)
        
        # Проверяем, нужно ли исправление
        if (-not (Test-HasGarbledText -Content $originalContent)) {
            return $false
        }
        
        Write-Host "  Обнаружены проблемы с кодировкой" -ForegroundColor Yellow
        
        # Пробуем разные кодировки
        foreach ($encoding in $encodings) {
            try {
                $decodedContent = $encoding.GetString($bytes)
                
                # Проверяем, исправилось ли
                if (-not (Test-HasGarbledText -Content $decodedContent)) {
                    # Сохраняем в UTF-8 без BOM
                    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
                    [System.IO.File]::WriteAllText($FilePath, $decodedContent, $utf8NoBom)
                    
                    Write-Host "  ✓ Исправлено с помощью $($encoding.EncodingName)" -ForegroundColor Green
                    return $true
                }
            }
            catch {
                continue
            }
        }
        
        Write-Host "  ✗ Не удалось автоматически исправить" -ForegroundColor Red
        return $false
    }
    catch {
        Write-Host "  ✗ Ошибка: $_" -ForegroundColor Red
        return $false
    }
}

# Поиск всех Java файлов
Write-Host "Поиск Java файлов..." -ForegroundColor Cyan
$javaFiles = Get-ChildItem -Path "." -Filter "*.java" -Recurse -ErrorAction SilentlyContinue | 
    Where-Object { $_.FullName -notmatch '\\build\\' -and $_.FullName -notmatch '\\.gradle\\' }

Write-Host "Найдено файлов: $($javaFiles.Count)" -ForegroundColor White
Write-Host ""

foreach ($file in $javaFiles) {
    $totalFiles++
    $relativePath = $file.FullName.Replace((Get-Location).Path, "").TrimStart('\')
    
    Write-Host "[$totalFiles/$($javaFiles.Count)] $relativePath" -ForegroundColor Gray
    
    if (Fix-FileEncoding -FilePath $file.FullName) {
        $fixedFiles++
    } else {
        $skippedFiles++
    }
}

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                    РЕЗУЛЬТАТЫ                          ║" -ForegroundColor Cyan
Write-Host "╠════════════════════════════════════════════════════════╣" -ForegroundColor Cyan
Write-Host "║  Всего проверено:  $($totalFiles.ToString().PadLeft(3)) файлов                        ║" -ForegroundColor White
Write-Host "║  Исправлено:       $($fixedFiles.ToString().PadLeft(3)) файлов                        ║" -ForegroundColor Green
Write-Host "║  Пропущено:        $($skippedFiles.ToString().PadLeft(3)) файлов                        ║" -ForegroundColor Yellow
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

if ($fixedFiles -gt 0) {
    Write-Host "✓ Исправление завершено успешно!" -ForegroundColor Green
    Write-Host "  Рекомендуется пересобрать проект командой:" -ForegroundColor Yellow
    Write-Host "  ./gradlew clean build" -ForegroundColor White
} else {
    Write-Host "✓ Все файлы уже в правильной кодировке!" -ForegroundColor Green
}

Write-Host ""
Read-Host "Нажмите Enter для выхода"
