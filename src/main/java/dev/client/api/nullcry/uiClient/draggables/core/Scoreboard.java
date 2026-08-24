package dev.client.api.nullcry.uiClient.draggables.core;

import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.events.core.render.RenderEvent;
import dev.client.api.nullcry.helper.other.DraggableHandler;
import dev.client.api.nullcry.render.ColorUtils;
import dev.client.api.nullcry.render.ScissorUtil;
import dev.client.api.nullcry.render.core.animations.nova.CompactAnimation;
import dev.client.api.nullcry.render.core.animations.nova.Easing;
import dev.client.api.nullcry.uiClient.draggables.HelperElements;
import dev.client.api.nullcry.uiClient.draggables.IHelper;
import dev.client.modules.core.misc.NameProtect;
import dev.client.modules.core.render.Interface;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Scoreboard implements IHelper {
    final DraggableHandler draggableHandler;

    public CompactAnimation widthAnim = new CompactAnimation(Easing.EASE_OUT_EXPO, 350);
    public CompactAnimation heightAnim = new CompactAnimation(Easing.EASE_OUT_EXPO, 350);
    public CompactAnimation showAnim = new CompactAnimation(Easing.EASE_OUT_CUBIC, 300);

    public Scoreboard() {
        this.draggableHandler = addDraggable("Scoreboard", 840, 200);
        widthAnim.setValue(draggableHandler.getWidth());
        heightAnim.setValue(draggableHandler.getHeight());
        showAnim.setValue(0.0);
    }

    float width, height;

    @Override
    public void onRender(RenderEvent.Draw2D event) {
        if (!dev.client.modules.core.hud.HudModuleHelper.isScoreboardEnabled()) return;
        if (mc.world == null) return;

        net.minecraft.scoreboard.Scoreboard sb = mc.world.getScoreboard();
        ScoreboardObjective objective = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);

        boolean visibleTarget = objective != null;
        showAnim.run(visibleTarget ? 1.0 : 0.0);
        showAnim.update();
        float show = (float) showAnim.getValue();
        if (show <= 0.001f || objective == null) return;

        NumberFormat numberFormat = objective.getNumberFormatOr(StyledNumberFormat.RED);
        List<ScoreboardEntry> base = sb.getScoreboardEntries(objective).stream()
                .filter(e -> !e.hidden())
                .sorted(Comparator
                        .comparingInt((ScoreboardEntry e) -> {
                            ReadableScoreboardScore rs = sb.getScore(ScoreHolder.fromName(e.owner()), objective);
                            return rs != null ? rs.getScore() : 0;
                        })
                        .reversed()
                        .thenComparing(ScoreboardEntry::owner, String::compareTo))
                .limit(15)
                .toList();

        record Row(Text name, Text score) {
        }
        List<Row> rows = new ArrayList<>();
        for (ScoreboardEntry e : base) {
            Team team = sb.getScoreHolderTeam(e.owner());
            Text name = Team.decorateName(team, e.name());
            Text score = e.formatted(numberFormat);
            rows.add(new Row(applyPlayerProtect(name), score));
        }

        Text title = applyAnarchyProtect(objective.getDisplayName());

        final float titleSize = 8.5f;
        final float lineSize = 8.0f;
        final float padX = 6f;
        final float padY = 6f;
        final float lineH = 9f;
        final float gapTitle = 3f;

        float maxWidth = Math.max(ClientApi.inter().getWidth(title, titleSize), 0f);
        float colonW = ClientApi.inter().getWidth(Text.literal(": "), lineSize);

        for (Row r : rows) {
            float nameW = ClientApi.inter().getWidth(r.name(), lineSize);
            float scoreW = ClientApi.inter().getWidth(r.score(), lineSize);
            float total = nameW + (scoreW > 0 ? colonW + scoreW : 0);
            if (total > maxWidth) maxWidth = total;
        }

        float boxW = maxWidth + padX * 6f;
        float boxH = padY + lineH + gapTitle + rows.size() * lineH + padY;

        widthAnim.run(boxW);
        heightAnim.run(boxH);
        widthAnim.update();
        heightAnim.update();

        float animW = (float) widthAnim.getValue();
        float animH = (float) heightAnim.getValue();
        draggableHandler.setWidth(animW);
        draggableHandler.setHeight(animH);
        width = animW;
        height = animH;

        float x = draggableHandler.getX();
        float y = draggableHandler.getY();

        HelperElements.rectElements(event.getContext(), x, y, animW, animH, show);

        float titleX = x + (animW - ClientApi.inter().getWidth(title, titleSize)) / 2f;
        float titleY = y + padY - 1f;
        int titleColor = ColorUtils.setAlpha(-1, (int) (255 * show));
        if (((titleColor >>> 24) & 0xFF) > 0 && ClientApi.inter().getWidth(title, titleSize) > 0.001f) {
            ClientApi.text().font(ClientApi.inter()).size(titleSize)
                    .color(titleColor)
                    .text(title)
                    .build()
                    .render(event.getContext().getMatrices().peek().getPositionMatrix(), titleX, titleY);
        }

        float nameX = x + padX;
        float rightX = x + animW - padX;
        float lineY = titleY + lineH + gapTitle;

        ScissorUtil.enableContext(event.getContext(), x, y, animW, animH);
        for (Row r : rows) {
            float slide = (1f - show) * 12f;
            int nameColor = ColorUtils.setAlpha(-1, (int) (255 * show));
            if (((nameColor >>> 24) & 0xFF) > 0 && ClientApi.inter().getWidth(r.name(), lineSize) > 0.001f) {
                ClientApi.text().font(ClientApi.inter()).size(lineSize)
                        .color(nameColor)
                        .text(r.name())
                        .build()
                        .render(event.getContext().getMatrices().peek().getPositionMatrix(), nameX - slide, lineY);
            }

            float scoreW = ClientApi.inter().getWidth(r.score(), lineSize);
            float colonX = rightX - scoreW - colonW + slide;
            float scoreX = rightX - scoreW + slide;
            if (scoreW > 0.001f) {
                int dimColor = ColorUtils.setAlpha(-1, (int) (220 * show));
                int scoreColor = ColorUtils.setAlpha(-1, (int) (255 * show));
                if (((dimColor >>> 24) & 0xFF) > 0) {
                    Text colon = Text.literal(": ");
                    if (ClientApi.inter().getWidth(colon, lineSize) > 0.001f) {
                        ClientApi.text().font(ClientApi.inter()).size(lineSize)
                                .color(dimColor)
                                .text(colon)
                                .build()
                                .render(event.getContext().getMatrices().peek().getPositionMatrix(), colonX, lineY);
                    }
                }

                if (((scoreColor >>> 24) & 0xFF) > 0 && ClientApi.inter().getWidth(r.score(), lineSize) > 0.001f) {
                    ClientApi.text().font(ClientApi.inter()).size(lineSize)
                            .color(scoreColor)
                            .text(r.score())
                            .build()
                            .render(event.getContext().getMatrices().peek().getPositionMatrix(), scoreX, lineY);
                }
            }

            lineY += lineH;
        }
        ScissorUtil.disableContext(event.getContext());
    }

    private static Text applyAnarchyProtect(Text in) {
        NameProtect np = NameProtect.INSTANCE;
        if (np.isEnabled() && np.anarchy.getEnabled()) {
            String replace = np.anarchyInput.getValue();
            if (!replace.isEmpty()) {
                if (!in.getSiblings().isEmpty()) {
                    Text res = Text.literal(" ").setStyle(in.getStyle());
                    for (Text sib : in.getSiblings()) {
                        String s = sib.getString();
                        if (s.contains("Анархия-")) {
                            String m = s.replaceAll("Анархия-\\d+", "Анархия-" + replace);
                            res.getSiblings().add(Text.literal(m).setStyle(sib.getStyle()));
                        } else {
                            res.getSiblings().add(sib.copy());
                        }
                    }
                    return res;
                } else {
                    String m = in.getString().replaceAll("Анархия-\\d+", "Анархия-" + replace);
                    return Text.literal(m).setStyle(in.getStyle());
                }
            }
        }
        return in;
    }

    private static Text applyPlayerProtect(Text name) {
        if (!NameProtect.INSTANCE.isEnabled()) return name;
        String me = MinecraftClient.getInstance().getGameProfile().getName();
        if (!name.getString().contains(me)) return name;

        if (!name.getSiblings().isEmpty()) {
            Text rebuilt = Text.literal(" ");
            for (Text part : name.getSiblings()) {
                String s = part.getString();
                if (s.contains(me)) {
                    rebuilt.getSiblings().add(Text.literal(s.replace(me, NameProtect.INSTANCE.nameClient)).setStyle(part.getStyle()));
                } else rebuilt.getSiblings().add(part);
            }
            return rebuilt;
        }
        return Text.literal(name.getString().replace(me, NameProtect.INSTANCE.nameClient)).setStyle(name.getStyle());
    }
}
