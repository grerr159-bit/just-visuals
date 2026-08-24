# Финальный скрипт для исправления всех проблем с кодировкой
# Версия 3.0 - Улучшенная обработка

param(
    [switch]$AutoFix = $false,
    [switch]$Verbose = $false
)

$ErrorActionPreference = "Continue"

Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   Финальное исправление кодировки v3.0                ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

$stats = @{
    Total = 0
    Fixed = 0
    Skipped = 0
    Errors = 0
}

# Известные исправления
$knownFixes = @{
    "SRPSpoof.java" = "Автоматически принимает серверные ресурс-паки"
    "CustomWorld.java" = "Изменяет время и туман в мире"
}

function Get-CleanDescription {
    param([string]$FileName)
    
    if ($knownFixes.ContainsKey($FileName)) {
        return $knownFixes[$FileName]
    }
    
    # Общие описания для разных типов модулей
    switch -Wildcard ($FileName) {
        "*Esp*" { return "Визуальные эффекты для отображения" }
        "*Hud*" { return "Элемент интерфейса" }
        "*World*" { return "Изменение параметров мира" }
        "*Render*" { return "Визуальные эффекты рендеринга" }
        "*Combat*" { return "Модуль для боя" }
        "*Movement*" { return "Модуль передвижения" }
        "*Player*" { return "Модуль игрока" }
        default { return "Модуль клиента" }
    }
}

function Test-HasEncodingIssues {
    param([string]$Content)
    
    # Проверяем различные признаки проблем с кодировкой
    $patterns = @(
        '\?\?\?',           # Три вопросительных знака
        '�',                # Символ замены
        '\?T\?',            # Паттерн с T
        '\? \? \?',         # Разделенные вопросительные знаки
        '????',             # Четыре вопросительных знака
        '\?\<\?',           # Паттерн с <
        '\?,\?',            # Паттерн с запятой
        "[\x00-\x08\x0B\x0C\x0E-\x1F]"  # Управляющие символы
    )
    
    foreach ($pattern in $patterns) {
        if ($Content -match $pattern) {
            return $true
        }
    }
    
    return $false
}

function Fix-JavaFile {
    param(
        [string]$FilePath,
        [string]$FileName
    )
    
    try {
        $content = [System.IO.File]::ReadAllText($FilePath, [System.Text.Encoding]::UTF8)
        
        if (-not (Test-HasEncodingIssues -Content $content)) {
            if ($Verbose) {
                Write-Host "  ✓ Файл в порядке" -ForegroundColor Green
            }
            return $false
        }
        
        Write-Host "  ⚠ Обнаружены проблемы с кодировкой" -ForegroundColor Yellow
        
        # Пробуем различные кодировки
        $encodings = @(
            [System.Text.Encoding]::GetEncoding(1251),  # Windows-1251
            [System.Text.Encoding]::GetEncoding(866),   # CP866
            [System.Text.Encoding]::GetEncoding("ISO-8859-5"),  # ISO Cyrillic
            [System.Text.Encoding]::GetEncoding("KOI8-R")  # KOI8-R
        )
        
        $bytes = [System.IO.File]::ReadAllBytes($FilePath)
        $fixed = $false
        
        foreach ($encoding in $encodings) {
            try {
                $decoded = $encoding.GetString($bytes)
                
                if (-not (Test-HasEncodingIssues -Content $decoded)) {
                    # Заменяем описание на чистое, если это известный файл
                    $cleanDesc = Get-CleanDescription -FileName $FileName
                    
                    if ($decoded -match 'super\("([^"]+)",\s*ModuleCategory\.\w+,\s*"[^"]*"\)') {
                        $moduleName = $matches[1]
                        $decoded = $decoded -replace 'super\("' + [regex]::Escape($moduleName) + '",\s*ModuleCategory\.\w+,\s*"[^"]*"\)', 
                                                     ('super("' + $moduleName + '", ModuleCategory.Visuals, "' + $cleanDesc + '")')
                    }
                    
                    # Сохраняем в UTF-8 без BOM
                    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
                    [System.IO.File]::WriteAllText($FilePath, $decoded, $utf8NoBom)
                    
                    Write-Host "  ✓ Исправлено ($($encoding.EncodingName))" -ForegroundColor Green
                    $fixed = $true
                    break
                }
            }
            catch {
                continue
            }
        }
        
        if (-not $fixed) {
            Write-Host "  ✗ Не удалось автоматически исправить" -ForegroundColor Red
            $stats.Errors++
        }
        
        return $fixed
    }
    catch {
        Write-Host "  ✗ Ошибка: $_" -ForegroundColor Red
        $stats.Errors++
        return $false
    }
}

# Поиск всех Java файлов
Write-Host "Сканирование проекта..." -ForegroundColor Cyan
$javaFiles = Get-ChildItem -Path "." -Filter "*.java" -Recurse -ErrorAction SilentlyContinue |
    Where-Object { 
        $_.FullName -notmatch '\\build\\' -and 
        $_.FullName -notmatch '\\.gradle\\' -and
        $_.FullName -notmatch '\\out\\' 
    }

Write-Host "Найдено файлов: $($javaFiles.Count)" -ForegroundColor White
Write-Host ""

foreach ($file in $javaFiles) {
    $stats.Total++
    $relativePath = $file.FullName.Replace((Get-Location).Path, "").TrimStart('\')
    
    Write-Host "[$($stats.Total)/$($javaFiles.Count)] $relativePath" -ForegroundColor Gray
    
    if (Fix-JavaFile -FilePath $file.FullName -FileName $file.Name) {
        $stats.Fixed++
    } else {
        $stats.Skipped++
    }
}

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║                    ИТОГОВЫЙ ОТЧЕТ                      ║" -ForegroundColor Cyan
Write-Host "╠════════════════════════════════════════════════════════╣" -ForegroundColor Cyan
Write-Host "║  Всего проверено:  $($stats.Total.ToString().PadLeft(3)) файлов                        ║" -ForegroundColor White
Write-Host "║  Исправлено:       $($stats.Fixed.ToString().PadLeft(3)) файлов                        ║" -ForegroundColor Green
Write-Host "║  Пропущено:        $($stats.Skipped.ToString().PadLeft(3)) файлов                        ║" -ForegroundColor Yellow
Write-Host "║  Ошибок:           $($stats.Errors.ToString().PadLeft(3)) файлов                        ║" -ForegroundColor Red
Write-Host "╚════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

if ($stats.Fixed -gt 0) {
    Write-Host "✓ Исправление завершено!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Рекомендуемые действия:" -ForegroundColor Yellow
    Write-Host "  1. Пересоберите проект: ./gradlew clean build" -ForegroundColor White
    Write-Host "  2. Проверьте исправленные файлы в IDE" -ForegroundColor White
    Write-Host "  3. Закоммитьте изменения в Git" -ForegroundColor White
} elseif ($stats.Errors -gt 0) {
    Write-Host "⚠ Обнаружены ошибки при обработке файлов" -ForegroundColor Yellow
    Write-Host "  Проверьте файлы вручную" -ForegroundColor White
} else {
    Write-Host "✓ Все файлы уже в правильной кодировке!" -ForegroundColor Green
}

Write-Host ""
Read-Host "Нажмите Enter для выхода"
