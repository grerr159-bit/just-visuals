package dev.client.api.injection;

import com.mojang.authlib.GameProfile;
import dev.client.Just;
import dev.client.api.nullcry.helper.client.irc.ClientLabelRegistry;
import dev.client.api.nullcry.helper.client.irc.IRClient;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.modules.core.misc.NameProtect;
import dev.client.modules.core.render.Interface;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.stream.Collectors;

import static dev.client.api.nullcry.ClientApi.mc;


@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixins {

    @Shadow
    @Nullable
    private Text header;

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    protected abstract List<PlayerListEntry> collectPlayerEntries();

    @Shadow
    public abstract Text getPlayerName(PlayerListEntry entry);

    @Shadow
    @Final
    private Map<UUID, PlayerListHud.Heart> hearts;

    @Shadow
    @Nullable
    private Text footer;

    @Shadow
    protected abstract void renderScoreboardObjective(ScoreboardObjective objective, int y, PlayerListHud.ScoreDisplayEntry scoreDisplayEntry, int left, int right, UUID uuid, DrawContext context);

    @Shadow
    protected abstract void renderLatencyIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry);

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRender(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, @Nullable ScoreboardObjective objective, CallbackInfo ci) {
        List<PlayerListEntry> list = this.collectPlayerEntries();
        List<PlayerListHud.ScoreDisplayEntry> list2 = new ArrayList<>(list.size());
        int i = this.client.textRenderer.getWidth(" ");
        int j = 0;
        int k = 0;
        ClientLabelRegistry labelRegistry = ClientLabelRegistry.getInstance();
        Map<UUID, String> labelByUuid = new HashMap<>();
        IRClient ircClient = Just.getInstance().getIRClient();
        boolean ircActive = ircClient != null && ircClient.isOpen();
        String localPlayerName = this.client.player != null ? this.client.player.getGameProfile().getName() : null;

        for (PlayerListEntry playerListEntry : list) {
            Text text = this.getPlayerName(playerListEntry);
            UUID uuid = playerListEntry.getProfile().getId();
            String profileName = playerListEntry.getProfile().getName();
            String playerLabel = null;
            if (ircActive) {
                playerLabel = labelRegistry.labelForPlayer(profileName);
                if ((playerLabel == null || playerLabel.isBlank()) && localPlayerName != null && localPlayerName.equalsIgnoreCase(profileName)) {
                    playerLabel = labelRegistry.getSelfLabel();
                }
            }
            labelByUuid.put(uuid, playerLabel);

            int labelWidth = 0;
            if (ircActive && playerLabel != null && !playerLabel.isBlank()) {
                Identifier icon = labelRegistry.textureForLabel(playerLabel);
                if (icon != null) {
                    labelWidth = labelRegistry.getIconWidth() + 2;
                } else {
                    labelWidth = this.client.textRenderer.getWidth("[" + playerLabel + "]") + 2;
                }
            }
            j = Math.max(j, this.client.textRenderer.getWidth(text) + labelWidth);
            int l = 0;
            Text text2 = null;
            int m = 0;
            if (objective != null) {
                ScoreHolder scoreHolder = ScoreHolder.fromProfile(playerListEntry.getProfile());
                ReadableScoreboardScore readableScoreboardScore = scoreboard.getScore(scoreHolder, objective);
                if (readableScoreboardScore != null) {
                    l = readableScoreboardScore.getScore();
                }

                if (objective.getRenderType() != ScoreboardCriterion.RenderType.HEARTS) {
                    NumberFormat numberFormat = objective.getNumberFormatOr(StyledNumberFormat.YELLOW);
                    text2 = ReadableScoreboardScore.getFormattedScore(readableScoreboardScore, numberFormat);
                    m = this.client.textRenderer.getWidth(text2);
                    k = Math.max(k, m > 0 ? i + m : 0);
                }
            }

            list2.add(new PlayerListHud.ScoreDisplayEntry(text, l, text2, m));
        }

        if (!this.hearts.isEmpty()) {
            Set<UUID> set = list.stream().map(e -> e.getProfile().getId()).collect(Collectors.toSet());
            this.hearts.keySet().removeIf((uuid) -> !set.contains(uuid));
        }

        int n = list.size();
        int o = n;

        int p;
        for (p = 1; o > 20; o = (n + p - 1) / p) {
            ++p;
        }

        boolean bl = this.client.isInSingleplayer() || this.client.getNetworkHandler().getConnection().isEncrypted();
        int q;
        if (objective != null) {
            if (objective.getRenderType() == ScoreboardCriterion.RenderType.HEARTS) {
                q = 90;
            } else {
                q = k;
            }
        } else {
            q = 0;
        }

        int m = Math.min(p * ((bl ? 9 : 0) + j + q + 13), scaledWindowWidth - 50) / p;
        int r = scaledWindowWidth / 2 - (m * p + (p - 1) * 5) / 2;
        int s = 10;
        int t = m * p + (p - 1) * 5;
        List<OrderedText> list3 = null;
        if (this.header != null && NameProtect.INSTANCE.isEnabled() && NameProtect.INSTANCE.anarchy.getEnabled()) {
            list3 = this.client.textRenderer.wrapLines(this.header, scaledWindowWidth - 50);
            String replace = NameProtect.INSTANCE.anarchyInput.getValue();

            if (!replace.isEmpty()) {
                Text newHeader = Text.literal("");

                for (Text sibling : this.header.getSiblings()) {
                    String siblingText = sibling.getString();

                    if (siblingText.contains("Анархия-")) {
                        String modifiedText = siblingText.replaceAll("Анархия-\\d+", "Анархия-" + replace);
                        newHeader.getSiblings().add(Text.literal(modifiedText).setStyle(sibling.getStyle()));
                    } else {
                        newHeader.getSiblings().add(sibling);
                    }
                }

                if (newHeader.getSiblings().isEmpty()) {
                    String modifiedText = this.header.getString().replaceAll("Анархия-\\d+", "Анархия-" + replace);
                    newHeader = Text.literal(modifiedText).setStyle(this.header.getStyle());
                }

                this.header = newHeader;
            }

            for (OrderedText orderedText : list3) {
                t = Math.max(t, this.client.textRenderer.getWidth(orderedText));
            }
        }

        List<OrderedText> list4 = null;
        if (this.footer != null) {
            list4 = this.client.textRenderer.wrapLines(this.footer, scaledWindowWidth - 50);

            for (OrderedText orderedText2 : list4) {
                t = Math.max(t, this.client.textRenderer.getWidth(orderedText2));
            }
        }

        if (list3 != null) {
            int var10001 = scaledWindowWidth / 2 - t / 2 - 1;
            int var10002 = s - 1;
            int var10003 = scaledWindowWidth / 2 + t / 2 + 1;
            int var10005 = list3.size();
            Objects.requireNonNull(this.client.textRenderer);
            context.fill(var10001, var10002, var10003, s + var10005 * 9, Integer.MIN_VALUE);

            for (OrderedText orderedText2 : list3) {
                int u = this.client.textRenderer.getWidth(orderedText2);
                context.drawTextWithShadow(this.client.textRenderer, orderedText2, scaledWindowWidth / 2 - u / 2, s, -1);
                Objects.requireNonNull(this.client.textRenderer);
                s += 9;
            }

            ++s;
        }

        context.fill(scaledWindowWidth / 2 - t / 2 - 1, s - 1, scaledWindowWidth / 2 + t / 2 + 1, s + o * 9, Integer.MIN_VALUE);
        int v = this.client.options.getTextBackgroundColor(553648127);

        for (int w = 0; w < n; ++w) {
            int u = w / o;
            int x = w % o;
            int y = r + u * m + u * 5;
            int z = s + x * 9;
            if (w < list.size()) {
                PlayerListEntry playerListEntry2 = (PlayerListEntry) list.get(w);
                PlayerListHud.ScoreDisplayEntry scoreDisplayEntry = (PlayerListHud.ScoreDisplayEntry) list2.get(w);
                GameProfile gameProfile = playerListEntry2.getProfile();
                String playerName = gameProfile.getName();

                boolean isPlayer = playerName.equals(mc.player.getName().getString());
                boolean isFriend = false;

                if (Just.getInstance().getFriendManager() != null) {
                    for (String friend : Just.getInstance().getFriendManager().getFriends()) {
                        if (friend.equalsIgnoreCase(playerName)) {
                            isFriend = true;
                            break;
                        }
                    }
                }

                int rectColor = v;

                if (NameProtect.INSTANCE.isEnabled() && isPlayer) {
                    rectColor = ColorUtils.rgba(0, 128, 200, 128);
                } else if (isFriend && NameProtect.INSTANCE.isEnabled() && NameProtect.INSTANCE.friend.getEnabled()) {
                    rectColor = ColorUtils.rgba(0, 128, 200, 128);
                }

                context.fill(y, z, y + m, z + 8, rectColor);

                if (bl) {
                    PlayerEntity playerEntity = this.client.world.getPlayerByUuid(gameProfile.getId());
                    boolean bl2 = playerEntity != null && LivingEntityRenderer.shouldFlipUpsideDown(playerEntity);
                    PlayerSkinDrawer.draw(context, playerListEntry2.getSkinTextures().texture(), y, z, 8, playerListEntry2.shouldShowHat(), bl2, -1);
                    y += 9;
                }

                int baseX = y;
                int textStart = baseX;
                String playerLabel = ircActive ? labelByUuid.get(gameProfile.getId()) : null;
                if (ircActive && playerLabel != null && !playerLabel.isBlank()) {
                    Identifier iconTexture = labelRegistry.textureForLabel(playerLabel);
                    if (iconTexture != null) {
                        int iconSize = labelRegistry.getIconWidth();
                        context.drawGuiTexture(
                                id -> RenderLayer.getGui(),
                                iconTexture,
                                textStart, z,
                                iconSize,
                                iconSize
                        );
                        textStart += iconSize + 2;
                    } else {
                        MutableText labelComponent = Text.literal("[" + playerLabel + "]");
                        int labelColor = Interface.INSTANCE.getMainColor();
                        context.drawTextWithShadow(this.client.textRenderer, labelComponent, textStart, z, labelColor);
                        textStart += this.client.textRenderer.getWidth(labelComponent) + 2;
                    }
                }

                Text chatComponent = this.getPlayerName(playerListEntry2);
                Text newComponent = Text.literal("");

                if (NameProtect.INSTANCE.isEnabled() && (isPlayer || (isFriend && NameProtect.INSTANCE.friend.getEnabled()))) {
                    if (!chatComponent.getSiblings().isEmpty()) {
                        for (Text sibling : chatComponent.getSiblings()) {
                            String replacementName = NameProtect.INSTANCE.nameClient;

                            if (sibling.getString().contains(playerName)) {
                                newComponent.getSiblings().add(Text.literal(sibling.getString().replaceAll(playerName, replacementName))
                                        .setStyle(sibling.getStyle()));
                            } else {
                                newComponent.getSiblings().add(sibling);
                            }
                        }
                    } else {
                        String replacementName = NameProtect.INSTANCE.nameClient;

                        newComponent = newComponent.copy().append(chatComponent.getString().replaceAll(playerName, replacementName));
                    }

                    context.drawTextWithShadow(this.client.textRenderer, newComponent, textStart, z, playerListEntry2.getGameMode() == GameMode.SPECTATOR ? -1862270977 : -1);
                } else {
                    context.drawTextWithShadow(this.client.textRenderer, chatComponent, textStart, z, playerListEntry2.getGameMode() == GameMode.SPECTATOR ? -1862270977 : -1);
                }
                if (objective != null && playerListEntry2.getGameMode() != GameMode.SPECTATOR) {
                    int aa = baseX + j + 1;
                    int ab = aa + q;
                    if (ab - aa > 5) {
                        this.renderScoreboardObjective(objective, z, scoreDisplayEntry, aa, ab, gameProfile.getId(), context);
                    }
                }

                this.renderLatencyIcon(context, m, baseX - (bl ? 9 : 0), z, playerListEntry2);
            }
        }

        if (list4 != null) {
            s += o * 9 + 1;
            int var55 = scaledWindowWidth / 2 - t / 2 - 1;
            int var56 = s - 1;
            int var57 = scaledWindowWidth / 2 + t / 2 + 1;
            int var58 = list4.size();
            Objects.requireNonNull(this.client.textRenderer);
            context.fill(var55, var56, var57, s + var58 * 9, Integer.MIN_VALUE);

            for (OrderedText orderedText3 : list4) {
                int x = this.client.textRenderer.getWidth(orderedText3);
                context.drawTextWithShadow(this.client.textRenderer, orderedText3, scaledWindowWidth / 2 - x / 2, s, -1);
                Objects.requireNonNull(this.client.textRenderer);
                s += 9;
            }
        }
        ci.cancel();
    }
}
