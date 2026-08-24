package dev.client.api.nullcry.uiClient.clickGui.api.setting;

import dev.client.api.nullcry.modules.settings.*;
import dev.client.api.nullcry.render.core.animations.client.Animation;
import dev.client.api.nullcry.render.core.animations.client.implement.DecelerateAnimation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

import java.util.Arrays;
import java.util.function.Supplier;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class Setting implements SettingLayer {
    String name;
    Supplier<Boolean> shown;
    Animation animation = new DecelerateAnimation().setMs(250).setValue(1);
    @NonFinal Object[] initialDefaults = null;

    @SuppressWarnings("unchecked")
    public <T extends Setting> T defaultValue(Object... values) {
        this.initialDefaults = (values != null) ? Arrays.copyOf(values, values.length) : null;

        Object first = (values != null && values.length > 0) ? values[0] : null;

        switch (this) {
            case CheckBox checkBox when first instanceof Boolean bool -> checkBox.applyDefault(bool);
            case Slider slider when first instanceof Number num -> slider.applyDefault(num.floatValue());
            case Input input when first instanceof String str -> input.set(str);
            case ModeElement mode when first instanceof String str -> mode.applyDefault(str);

            case SelectElements select -> {
                String[] strings = (values == null) ? new String[0] : Arrays.stream(values).map(String::valueOf).toArray(String[]::new);
                select.applyDefault(strings);
            }

            case ClickSetting click -> {}
            case ColorPicker color when first instanceof Integer intColor -> color.set(intColor);
            case KeyBind keyBind when first instanceof Integer key -> keyBind.set(key);

            default ->
                    throw new IllegalArgumentException("Невозможно установить defaultValue для " + this.getClass().getSimpleName() + " с типом " + (first != null ? first.getClass().getSimpleName() : "null"));
        }
        return (T) this;
    }


    public Object[] getInitialDefaults() {
        return initialDefaults == null ? null : Arrays.copyOf(initialDefaults, initialDefaults.length);
    }

    public void resetToDefault() {
        if (initialDefaults != null) {
            defaultValue(initialDefaults);
            return;
        }

        switch (this) {
            case CheckBox cb -> cb.applyDefault(false);
            case Slider sl ->
                    sl.applyDefault(sl.getMin() != null && sl.getMax() != null ? (sl.getMin() + sl.getMax()) / 2f : 0f);
            case Input in -> in.applyDefault("");
            case ModeElement me -> {
                String dv = me.getDefaultValue();
                if (dv != null && me.getValues().contains(dv)) me.applyDefault(dv);
                else if (!me.getValues().isEmpty()) me.set(me.getValues().getFirst());
                else me.set("");
            }
            case SelectElements se -> {
                for (CheckBox cb : se.getValues()) cb.applyDefault(false);
                for (CheckBox cb : se.getValues())
                    if (se.getDefaultSelected().contains(cb.getName())) cb.applyDefault(true);
            }
            case ClickSetting cs -> {}
            case ColorPicker cp -> cp.set(-1);
            case KeyBind kb -> kb.applyDefault(org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN);
            default -> {
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Setting> T register(SettingProvider provider) {
        provider.getSettings().add(this);
        return (T) this;
    }
}
