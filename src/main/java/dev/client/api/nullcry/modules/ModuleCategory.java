package dev.client.api.nullcry.modules;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ModuleCategory {

    Visuals('C'),
    HUD('F'),
    Utils('E'),
    Location('L'),
    Friends('R'),
    Configs('Q');

    char icon;

}
