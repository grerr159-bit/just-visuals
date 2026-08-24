package dev.client.component.core.client;

import com.google.common.eventbus.Subscribe;
import dev.client.Just;
import dev.client.api.nullcry.IModule;
import dev.client.api.nullcry.events.core.network.UpdateEvent;
import dev.client.api.nullcry.helper.client.ConnectionHelper;
import dev.client.api.nullcry.modules.Module;
import dev.client.component.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ModuleConnectionUpdate extends Component {
    private static final File STORAGE_FILE = new File(mc.runDirectory, "last_server.txt");

    private IModule.ServerType activeServer = null;

    @Subscribe
    public void onUpdate(UpdateEvent event) {
        IModule.ServerType current = getCurrentServer();

        if (current == null) {
            if (activeServer != null) {
                saveLastServer(activeServer);
            }
            activeServer = null;

            for (Module module : Just.getInstance().getModuleManager()) {
                if (module.isEnabled() && !module.getAllowedServers().isEmpty()) {
                    module.printClient("Р В Р’В Р РЋРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р СћРІР‚ВР В Р Р‹Р РЋРІР‚СљР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В Р вЂ° \"" + module.getName() + "\" Р В Р’В Р вЂ™Р’В±Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В» Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В РІР‚в„–Р В Р Р‹Р Р†Р вЂљР Р‹Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В РІР‚В¦: Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦ Р В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’ВµР В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚СћР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦ Р В Р’В Р В РІР‚В  Р В Р’В Р РЋРІР‚СћР В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚ВР В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљР Р‹Р В Р’В Р В РІР‚В¦Р В Р’В Р РЋРІР‚СћР В Р’В Р РЋР’В Р В Р’В Р РЋР’ВР В Р’В Р РЋРІР‚ВР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’Вµ");
                    module.toggle();
                }
            }
            return;
        }

        if (activeServer != current) {
            activeServer = current;
            saveLastServer(current);
        }

        for (Module module : Just.getInstance().getModuleManager()) {
            if (module.isEnabled()
                    && !module.getAllowedServers().isEmpty()
                    && !module.getAllowedServers().contains(current)) {
                module.printClient("Р В Р’В Р РЋРЎв„ўР В Р’В Р РЋРІР‚СћР В Р’В Р СћРІР‚ВР В Р Р‹Р РЋРІР‚СљР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В Р вЂ° \"" + module.getName() + "\" Р В Р’В Р вЂ™Р’В±Р В Р Р‹Р Р†Р вЂљРІвЂћвЂ“Р В Р’В Р вЂ™Р’В» Р В Р’В Р РЋРІР‚СћР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р’В Р РЋРІР‚СњР В Р’В Р вЂ™Р’В»Р В Р Р‹Р В РІР‚в„–Р В Р Р‹Р Р†Р вЂљР Р‹Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р В РІР‚В¦: Р В Р’В Р РЋРІР‚СћР В Р’В Р В РІР‚В¦ Р В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’ВµР В Р’В Р СћРІР‚ВР В Р’В Р РЋРІР‚СћР В Р Р‹Р В РЎвЂњР В Р Р‹Р Р†Р вЂљРЎв„ўР В Р Р‹Р РЋРІР‚СљР В Р’В Р РЋРІР‚вЂќР В Р’В Р вЂ™Р’ВµР В Р’В Р В РІР‚В¦ Р В Р’В Р В РІР‚В¦Р В Р’В Р вЂ™Р’В° Р В Р Р‹Р В РЎвЂњР В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р В РІР‚В Р В Р’В Р вЂ™Р’ВµР В Р Р‹Р В РІР‚С™Р В Р’В Р вЂ™Р’Вµ " + current.name());
                module.toggle();
            }
        }
    }

    private IModule.ServerType getCurrentServer() {
        String ip = ConnectionHelper.getServerIP();
        if (ip == null || ip.isEmpty() || ip.equals("local")) return null;

        for (IModule.ServerType type : IModule.ServerType.values()) {
            if (!type.getId().isEmpty() && ip.contains(type.getId())) {
                return type;
            }
        }
        return null;
    }

    private void saveLastServer(IModule.ServerType type) {
        try {
            File parent = STORAGE_FILE.getParentFile();
            if (parent != null) parent.mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(STORAGE_FILE))) {
                writer.write(type.name());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
