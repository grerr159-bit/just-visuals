package dev.client.api.nullcry.modules.settings;

import dev.client.api.nullcry.uiClient.clickGui.api.setting.Setting;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.function.Supplier;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Slider extends Setting {
    @Setter Boolean dragging = false;
    public Float value, min, max, increments;

    public Slider(String name, Supplier<Boolean> visible) {
        super(name, visible);
    }

    public Slider set(float min, float max, float increments) {
        this.min = min;
        this.max = max;
        this.increments = increments;
        this.applyDefault(max / 2);

        return this;
    }

    public Slider applyDefault(float value) {
        this.value = Math.max(min, Math.min(max, value));
        return this;
    }

    @Override
    public Slider collection(Collection collection) {
        collection.put(this);

        return this;
    }
}
