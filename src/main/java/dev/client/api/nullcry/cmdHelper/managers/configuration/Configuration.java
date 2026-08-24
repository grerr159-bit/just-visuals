package dev.client.api.nullcry.cmdHelper.managers.configuration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.client.Just;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.modules.settings.*;
import dev.client.api.nullcry.uiClient.clickGui.api.setting.Setting;
import lombok.Getter;

import java.io.File;
import java.util.function.Consumer;

@Getter
public class Configuration implements ClientApi {
    private final File file;
    private final String name;

    public Configuration(String name, File file) {
        this.name = name;
        this.file = file;
    }

    public void loadConfig(JsonObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        if (jsonObject.has("modules")) {
            loadModulesSettings(jsonObject.getAsJsonObject("modules"));
        }
    }

    private void loadModulesSettings(JsonObject modulesObject) {
        Just.getInstance().getModuleManager().getModules().forEach(f -> {
            JsonObject moduleObject = modulesObject.getAsJsonObject(f.getName().toLowerCase());
            if (moduleObject == null) {
                return;
            }

            f.setEnabled(false);
            loadSettingFromJson(moduleObject, "bind", value -> f.setKey(value.getAsInt()));
            loadSettingFromJson(moduleObject, "enable", value -> f.setEnabled(value.getAsBoolean()));

            f.getSettings().forEach(setting -> loadIndividualSetting(moduleObject, setting));
        });
    }

    private void loadIndividualSetting(JsonObject moduleObject, Setting setting) {
        JsonElement settingElement = moduleObject.get(setting.getName());
        if (settingElement == null || settingElement.isJsonNull()) {
            return;
        }

        try {
            switch (setting) {
                case Slider slider -> slider.applyDefault(settingElement.getAsFloat());
                case CheckBox checkBox -> checkBox.applyDefault(settingElement.getAsBoolean());
                case ColorPicker colorPicker -> colorPicker.set(settingElement.getAsInt());
                case ModeElement modeElement -> modeElement.set(settingElement.getAsString());
                case KeyBind keyBind -> keyBind.set(settingElement.getAsInt());
                case SelectElements selectElements -> loadSelectElements(selectElements, settingElement.getAsJsonObject());
                case Input input -> input.set(settingElement.getAsString());
                case Collection collection -> {
                    JsonObject nested = settingElement.getAsJsonObject();
                    collection.getSettings().forEach(subSetting -> loadIndividualSetting(nested, subSetting));
                }
                default -> {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadSelectElements(SelectElements setting, JsonObject elements) {
        setting.getValues().forEach(option -> {
            JsonElement optionElement = elements.get(option.getName());
            if (optionElement != null && !optionElement.isJsonNull()) {
                option.applyDefault(optionElement.getAsBoolean());
            }
        });
    }

    private void loadSettingFromJson(JsonObject jsonObject, String key, Consumer<JsonElement> consumer) {
        JsonElement element = jsonObject.get(key);
        if (element != null && !element.isJsonNull()) {
            consumer.accept(element);
        }
    }

    public JsonElement saveConfig() {
        JsonObject modulesObject = new JsonObject();

        saveModuleSettings(modulesObject);
        JsonObject newObject = new JsonObject();
        newObject.add("modules", modulesObject);

        return newObject;
    }

    private void saveModuleSettings(JsonObject modulesObject) {
        Just.getInstance().getModuleManager().getModules().forEach(module -> {
            JsonObject moduleObject = new JsonObject();

            moduleObject.addProperty("bind", module.getKey());
            moduleObject.addProperty("enable", module.isEnabled());

            module.getSettings().forEach(setting -> saveIndividualSetting(moduleObject, setting));
            modulesObject.add(module.getName().toLowerCase(), moduleObject);
        });
    }

    private void saveIndividualSetting(JsonObject moduleObject, Setting setting) {
        if (setting instanceof CheckBox) {
            moduleObject.addProperty(setting.getName(), ((CheckBox) setting).getEnabled());
        } else if (setting instanceof Slider) {
            moduleObject.addProperty(setting.getName(), ((Slider) setting).getValue());
        } else if (setting instanceof ModeElement) {
            moduleObject.addProperty(setting.getName(), ((ModeElement) setting).getValue());
        } else if (setting instanceof ColorPicker) {
            int argb = ((ColorPicker) setting).getColorRGBA();
            moduleObject.addProperty(setting.getName(), argb);
        } else if (setting instanceof KeyBind) {
            moduleObject.addProperty(setting.getName(), ((KeyBind) setting).getKey());
        } else if (setting instanceof SelectElements) {
            saveMultiSetting(moduleObject, (SelectElements) setting);
        } else if (setting instanceof Input) {
            moduleObject.addProperty(setting.getName(), ((Input) setting).getValue());
        } else if (setting instanceof Collection) {
            JsonObject nestedSettings = new JsonObject();
            ((Collection) setting).getSettings()
                    .forEach(subSetting -> saveIndividualSetting(nestedSettings, subSetting));
            moduleObject.add(setting.getName(), nestedSettings);
        }
    }

    private void saveMultiSetting(JsonObject moduleObject, SelectElements setting) {
        JsonObject elements = new JsonObject();
        setting.getValues().forEach(option -> elements.addProperty(option.getName(), option.getEnabled()));
        moduleObject.add(setting.getName(), elements);
    }
}

