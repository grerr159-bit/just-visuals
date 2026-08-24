package dev.client;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.ClientInfo;
import dev.client.api.nullcry.cmdHelper.ConsoleLogger;
import dev.client.api.nullcry.cmdHelper.MinecraftLogger;
import dev.client.api.nullcry.cmdHelper.MultiLogger;
import dev.client.api.nullcry.cmdHelper.interfaces.CommandDispatcher;
import dev.client.api.nullcry.cmdHelper.interfaces.Logger;
import dev.client.api.nullcry.cmdHelper.managers.configuration.ConfigurationManager;
import dev.client.api.nullcry.cmdHelper.managers.dragHandler.DraggableManager;
import dev.client.api.nullcry.cmdHelper.managers.friend.FriendManager;
import dev.client.api.nullcry.cmdHelper.managers.macro.MacroManager;
import dev.client.api.nullcry.events.PriorityEventBus;
import dev.client.api.nullcry.events.core.input.KeyBindEvent;
import dev.client.api.nullcry.helper.client.irc.IRClient;
import dev.client.api.nullcry.helper.client.server.ServerUtils;
import dev.client.api.nullcry.helper.other.Console;
import dev.client.api.nullcry.modules.ModuleManager;
import dev.client.api.nullcry.modules.listener.ListenerRepository;
import dev.client.api.nullcry.updater.Updater;
import dev.client.api.nullcry.uiClient.altManager.AltConfiguration;
import dev.client.api.nullcry.uiClient.altManager.AltScreenManager;
import dev.client.api.nullcry.uiClient.clickGui.NewClickGuiScreen;
import dev.client.api.nullcry.uiClient.notification.NotificationManager;
import dev.client.cmd.core.Cmd_Initializer;
import dev.client.component.ComponentManager;
import dev.other.DiscordManager;
import dev.other.customUser.ProfileType;
import dev.other.customUser.UserProfile;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.session.Session;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Uuids;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Just implements ModInitializer, ClientApi {
    public final BuildType mode = BuildType.DEVELOPMENT;

    public static final List<String> customServer = List.of(
            "FunTime:mc.funtime.su",
            "HolyWorld:start.holyworld.ru",
            "ReallyWorld:mc.reallyworld.ru",
            "SpookyTime:spookytime.net",
            "MetaHVH:mc.metahvh.space"
    );

    public static ArrayList<Packet<?>> silentPackets = new ArrayList<>();

    @Getter public static float tickTimer = 1.0f;
    @Getter public static float jumpTicks = 0.02F;

    @Getter @Setter private volatile boolean isFocusedRender = false;

    public boolean customTitles() {
        return true;
    }

    public boolean cryptEnabled() {
        return mode == BuildType.BUILD;
    }

    public boolean development() {
        return mode == BuildType.DEVELOPMENT;
    }

    @Getter private static Just instance;
    final PriorityEventBus eventBus = new PriorityEventBus();
    final Logger loggerCMD = new MultiLogger(List.of(new ConsoleLogger(), new MinecraftLogger()));

    final File clientDir = new File(mc.runDirectory + "\\Just");
    final File filesDir = new File(clientDir, "files");

    ClientInfo clientInfo;
    UserProfile userProfile;
    DiscordManager discordManager;

    ConfigurationManager configurationManager;
    AltConfiguration altConfiguration;
    DraggableManager draggableManager;
    FriendManager friendManager;
    MacroManager macroManager;

    ModuleManager moduleManager;
    ComponentManager componentManager;

    CommandDispatcher commandDispatcher;
    Cmd_Initializer cmdInitializer;

    NewClickGuiScreen clickGuiScreen;
    AltScreenManager altScreenManager;
    NotificationManager notificationManager;

    ListenerRepository listenerRepository;
    ServerUtils serverUtils;
    @Setter IRClient IRClient;

    public Just() {
        instance = this;
        createDirectories();
        init();
        eventBus.register(this);
    }

    private void createDirectories() {
        if (!clientDir.exists()) clientDir.mkdirs();
        if (!filesDir.exists()) filesDir.mkdirs();
    }

    public void init() {
        initClientServices();
        registerManagers();
        initModules();
        initListeners();
        initScreens();
        initCMD();
        initManagers();
        this.clickGuiScreen = new NewClickGuiScreen();

        Updater.INSTANCE.init();
    }

    void initClientServices() {
        clientInfo = new ClientInfo("Just", "1.0.0", "/assets/Just/", mode == BuildType.BUILD ? ClientInfo.BuildType.Beta : ClientInfo.BuildType.Private);
        userProfile = ProfileType.Developer.createProfile();

        discordManager = new DiscordManager();
    }

    void registerManagers() {
        configurationManager = new ConfigurationManager();
        altConfiguration = new AltConfiguration();
        draggableManager = new DraggableManager();
        friendManager = new FriendManager();
        macroManager = new MacroManager();

        serverUtils = new ServerUtils();

        IRClient = null;
    }

    void initModules() {
        moduleManager = new ModuleManager();
        moduleManager.init();
        componentManager = new ComponentManager();
        componentManager.initComponents();
    }

    void initScreens() {
        altScreenManager = new AltScreenManager();
        notificationManager = new NotificationManager();
    }

    private void initCMD() {
        cmdInitializer = new Cmd_Initializer(mc, loggerCMD, macroManager, configurationManager);
        commandDispatcher = cmdInitializer.getCommandDispatcher();
    }

    private void initManagers() {
        try {
            configurationManager.init();
            altConfiguration.init();
            {
                String sel = AltConfiguration.getSelected();
                if (sel != null && !sel.isBlank()) {
                    String n = sel.trim();
                    mc.session = new Session(
                            n,
                            Uuids.getOfflinePlayerUuid(n),
                            "0",
                            Optional.empty(),
                            Optional.of("offline"),
                            Session.AccountType.LEGACY
                    );
                }
            }

            friendManager.init();
            macroManager.init();
            dev.client.api.nullcry.uiClient.clickGui.newgui.FriendsStorage.load();
            draggableManager.init();
            draggableManager.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initListeners() {
        listenerRepository = new ListenerRepository();
        listenerRepository.setup();
    }

    final KeyBindEvent keyBindEvent = new KeyBindEvent(-1);

    public void keyPressed(int key) {
        keyBindEvent.setKey(key);
        eventBus.post(keyBindEvent);
        macroManager.onKey(key);
    }

    public static void openLink(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec("cmd /c start " + url);
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open " + url);
            } else {
                Runtime.getRuntime().exec("xdg-open " + url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public enum BuildType {
        BUILD,
        DEVELOPMENT
    }

    public interface DelayHolder {
        double Just$getThrowInventoryScreenDelay();
        void Just$setThrowInventoryScreenDelay(double value);

        double Just$getChestScreenDelay();
        void Just$setChestScreenDelay(double v);
    }

    @Override
    public void onInitialize() {
        Console.log("Client was successfully initialized");
    }
}
