$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$files = Get-ChildItem -Path "src/main/java/dev/client/modules/core" -Recurse -Filter "*.java"

foreach ($file in $files) {
    $bytes = [System.IO.File]::ReadAllBytes($file.FullName)
    
    # робуем декодировать как UTF-8 с ошибками
    $utf8 = [System.Text.Encoding]::UTF8
    $text = $utf8.GetString($bytes)
    
    # сли есть испорченные символы, пробуем восстановить через Windows-1251
    if ($text -match '\?') {
        # итаем как Latin1 чтобы получить оригинальные байты
        $latin1 = [System.Text.Encoding]::GetEncoding(28591)
        $win1251 = [System.Text.Encoding]::GetEncoding(1251)
        
        $originalBytes = $latin1.GetBytes($text)
        $restored = $win1251.GetString($originalBytes)
        
        [System.IO.File]::WriteAllText($file.FullName, $restored, $utf8NoBom)
        Write-Host "Restored: $($file.Name)"
    }
}
