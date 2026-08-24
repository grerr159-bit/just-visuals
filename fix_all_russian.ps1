# Script to fix all corrupted Russian descriptions in Java module files

$fixes = @{
    "src/main/java/dev/client/modules/core/misc/AutoUse.java" = @{
        old = 'super("AutoUse", ModuleCategory.Utils, ".*?")'
        new = 'super("AutoUse", ModuleCategory.Utils, "Автоматическое использование еды, зелий невидимости и золотых яблок")'
    }
    "src/main/java/dev/client/modules/core/misc/ClickHelper.java" = @{
        old = 'super("ClickHelper", ModuleCategory.Utils, ".*?")'
        new = 'super("ClickHelper", ModuleCategory.Utils, "Автоматическое использование эндер-жемчуга и добавление в друзья нажатием клавиш")'
    }
    "src/main/java/dev/client/modules/core/misc/ElytraHelper.java" = @{
        old = 'super("ElytraHelper", ModuleCategory.Utils, ".*?")'
        new = 'super("ElytraHelper", ModuleCategory.Utils, "Автоматическое использование элитр, фейерверков, замена нагрудника и ускорение элитр")'
    }
    "src/main/java/dev/client/modules/core/render/CustomWorld.java" = @{
        old = 'super("Ambience", ModuleCategory.Visuals, ".*?")'
        new = 'super("Ambience", ModuleCategory.Visuals, "Позволяет изменить время суток и настроить туман для создания атмосферы")'
    }
    "src/main/java/dev/client/modules/core/misc/ReallyWorldHelper.java" = @{
        old = 'super("ReallyWorldHelper", ModuleCategory.Utils, ".*?")'
        new = 'super("ReallyWorldHelper", ModuleCategory.Utils, "Вспомогательные функции для игры на сервере ReallyWorld")'
    }
    "src/main/java/dev/client/modules/core/misc/AutoAuth.java" = @{
        old = 'super("AutoAuth", ModuleCategory.Utils, ".*?")'
        new = 'super("AutoAuth", ModuleCategory.Utils, "Автоматическое использование регистрации или авторизации на сервере")'
    }
    "src/main/java/dev/client/modules/core/misc/AutoAccept.java" = @{
        old = 'super("AutoAccept", ModuleCategory.Utils, ".*?")'
        new = 'super("AutoAccept", ModuleCategory.Utils, "Автоматически принимает запросы на телепортацию, дуэли и приглашения в кланы")'
    }
    "src/main/java/dev/client/modules/core/misc/AutoLeave.java" = @{
        old = 'super("AutoLeave", ModuleCategory.Utils, ".*?")'
        new = 'super("AutoLeave", ModuleCategory.Utils, "Автоматически покидает сервер при обнаружении игроков в радиусе или при низком здоровье")'
    }
    "src/main/java/dev/client/modules/core/misc/PotionCombiner.java" = @{
        old = 'super("PotionCombiner", ModuleCategory.Utils, ".*?")'
        new = 'super("PotionCombiner", ModuleCategory.Utils, "Автоматическое объединение зелий силы или скорости на наковальне")'
    }
    "src/main/java/dev/client/modules/core/render/Particles.java" = @{
        old = 'super("Particles", ModuleCategory.Visuals, ".*?")'
        new = 'super("Particles", ModuleCategory.Visuals, "Отображает визуальные эффекты вокруг цели, при движении, атаке и различных игровых событиях")'
    }
    "src/main/java/dev/client/modules/core/misc/LockSlot.java" = @{
        old = 'super("LockSlot", ModuleCategory.Utils, ".*?")'
        new = 'super("LockSlot", ModuleCategory.Utils, "Не даёт выбросить предметы из определённых слотов или выбрасывать любые предметы")'
    }
    "src/main/java/dev/client/modules/core/misc/ServerHelper.java" = @{
        old = 'super("ServerHelper", ModuleCategory.Utils, ".*?")'
        new = 'super("ServerHelper", ModuleCategory.Utils, "Вспомогательные функции для игры на серверах FunTime и HolyWorld")'
    }
    "src/main/java/dev/client/modules/core/misc/IRC.java" = @{
        old = 'super("IRC", ModuleCategory.Utils, ".*?")'
        new = 'super("IRC", ModuleCategory.Utils, "Позволяет общаться с другими игроками через IRC-клиент и размещать маркеры на карте")'
    }
    "src/main/java/dev/client/modules/core/misc/AHHelper.java" = @{
        old = 'super("AH Helper", ModuleCategory.Utils, ".*?")'
        new = 'super("AH Helper", ModuleCategory.Utils, "Помощник для аукциона")'
    }
    "src/main/java/dev/client/modules/core/misc/ActionDetect.java" = @{
        old = 'super("ActionDetect", ModuleCategory.Utils, ".*?")'
        new = 'super("ActionDetect", ModuleCategory.Utils, "Отслеживает действия игроков")'
    }
    "src/main/java/dev/client/modules/core/misc/AutoDuel.java" = @{
        old = 'super("AutoDuel", ModuleCategory.Utils, ".*?")'
        new = 'super("AutoDuel", ModuleCategory.Utils, "Автоматическое использование дуэлей")'
    }
    "src/main/java/dev/client/modules/core/misc/AutoClanUpgrade.java" = @{
        old = 'super("AutoClanUpgrade", ModuleCategory.Utils, ".*?")'
        new = 'super("AutoClanUpgrade", ModuleCategory.Utils, "Автоматическое использование улучшения клана")'
    }
    "src/main/java/dev/client/modules/core/misc/AutoBrewPotion.java" = @{
        old = 'super("AutoBrewPotion", ModuleCategory.Utils, ".*?")'
        new = 'super("AutoBrewPotion", ModuleCategory.Utils, "Автоматическое использование варки зелий")'
    }
}

$encoding = [System.Text.UTF8Encoding]::new($false)

foreach ($file in $fixes.Keys) {
    if (Test-Path $file) {
        Write-Host "Fixing $file..." -ForegroundColor Yellow
        
        $content = [System.IO.File]::ReadAllText($file, $encoding)
        $pattern = $fixes[$file].old
        $replacement = $fixes[$file].new
        
        $newContent = $content -replace $pattern, $replacement
        
        [System.IO.File]::WriteAllText($file, $newContent, $encoding)
        
        Write-Host "Fixed $file" -ForegroundColor Green
    } else {
        Write-Host "File not found: $file" -ForegroundColor Red
    }
}

Write-Host "`nAll files have been processed!" -ForegroundColor Cyan
