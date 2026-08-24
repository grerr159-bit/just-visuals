package dev.client.api.injection;

import dev.client.Just;
import dev.client.api.nullcry.helper.client.ConnectionHelper;
import dev.client.api.nullcry.uiClient.notification.type.NotificationType;
import dev.client.api.nullcry.uiClient.notification.type.RenderType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixins extends Screen {

    protected GameMenuScreenMixins(Text title) {
        super(title);
    }

    @Inject(method = "initWidgets", at = @At("HEAD"))
    private void onInitWidgets(CallbackInfo ci) {
        String reconnect = "Reconnect";
        if (this.client.getLanguageManager().getLanguage().equals("ru_ru")) {
            reconnect = "Переподключиться";
        }

        int btnWidth = 204;
        int btnHeight = 20;
        int x = this.width / 2 - btnWidth / 2;
        int y = (int) (this.height * 0.25f) + 126;

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal(reconnect), btn -> {
                    if (ConnectionHelper.isPvP()) {
                        Just.getInstance().getNotificationManager().add("Вы не можете выйти в PvP режиме!", 3, NotificationType.WARNING, RenderType.SCREEN);
                        return;
                    }

                    ServerInfo serverData = this.client.getCurrentServerEntry();
                    if (serverData == null) {
                        Just.getInstance().getNotificationManager().add("Вы не подключены к серверу!", 3, NotificationType.INFO, RenderType.SCREEN);
                        return;
                    }

                    if (this.client.world == null) {
                        Just.getInstance().getNotificationManager().add("Мир не загружен. Попробуйте ещё раз.", 3, NotificationType.INFO, RenderType.SCREEN);
                        return;
                    }

                    this.client.world.disconnect();
                    this.client.disconnect();

                    Screen parentScreen = new MultiplayerScreen(new TitleScreen());

                    String addr = serverData.address != null ? serverData.address.trim() : "";
                    if (!ServerAddress.isValid(addr)) {
                        Just.getInstance().getNotificationManager().add("Некорректный адрес сервера.", 3, NotificationType.INFO, RenderType.SCREEN);
                        return;
                    }

                    ServerAddress serverAddress = ServerAddress.parse(addr);
                    ConnectScreen.connect(parentScreen, this.client, serverAddress, serverData, false, null);
                }).dimensions(x, y, btnWidth, btnHeight).build()
        );
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!Just.getInstance().getNotificationManager().notificationRenders.isEmpty()) {
            Just.getInstance().getNotificationManager().draw(context, context.getMatrices(), RenderType.SCREEN);
        }
    }

    @Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
    private void onDisconnectIntercept(CallbackInfo ci) {
        if (!ConnectionHelper.isPvP()) return;

        boolean ru = this.client != null
                && this.client.getLanguageManager().getLanguage().equals("ru_ru");

        Text body = Text.literal(ru
                ? "Вы находитесь в PvP режиме. Вы точно хотите выйти?"
                : "You are in PvP mode. Are you sure you want to disconnect?");
        Text yes  = Text.literal(ru ? "Подтвердить" : "Confirm");
        Text no   = Text.literal(ru ? "Назад" : "Back");

        this.client.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                disconnectVanilla();
            } else {
                this.client.setScreen((Screen) (Object) this);
            }
        }, Text.empty(), body, yes, no));

        ci.cancel();
    }

    @Unique
    private void disconnectVanilla() {
        if (this.client == null) return;

        boolean singleplayer = this.client.isInSingleplayer();
        ServerInfo serverInfo = this.client.getCurrentServerEntry();

        if (this.client.world != null) {
            this.client.world.disconnect();
        }

        if (singleplayer) {
            this.client.disconnect(new MessageScreen(Text.translatable("menu.savingLevel")));
        } else {
            this.client.disconnect();
        }

        TitleScreen titleScreen = new TitleScreen();
        if (singleplayer) {
            this.client.setScreen(titleScreen);
        } else if (serverInfo != null && serverInfo.isRealm()) {
            this.client.setScreen(new RealmsMainScreen(titleScreen));
        } else {
            this.client.setScreen(new MultiplayerScreen(titleScreen));
        }
    }
}
