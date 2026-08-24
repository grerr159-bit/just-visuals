package dev.client.modules.core.misc;

import com.google.common.eventbus.Subscribe;
import com.mojang.datafixers.util.Pair;
import dev.client.api.nullcry.events.core.input.KeyBindEvent;
import dev.client.api.nullcry.events.core.other.ScoreBoardEvent;
import dev.client.api.nullcry.helper.player.ItemUtil;
import dev.client.api.nullcry.modules.Module;
import dev.client.api.nullcry.modules.ModuleCategory;
import dev.client.api.nullcry.modules.settings.KeyBind;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class AHHelper extends Module {
    public static AHHelper INSTANCE;

    public AHHelper() {
        super("AH Helper", ModuleCategory.Utils, "Помощник для аукциона");
        INSTANCE = this;
    }

    public final KeyBind openAuctionBind = new KeyBind("Бинд открытия аукциона", () -> true)
            .set(GLFW.GLFW_KEY_UNKNOWN)
            .applyDefault(GLFW.GLFW_KEY_UNKNOWN)
            .register(this);

    private int balance = -1;

    private Slot cheapestSlot;
    private Slot bestUnitSlot;
    private Slot additionalUnitSlot;
    private boolean auctionScreenActive = false;

    private int cheapestPrice = Integer.MAX_VALUE;
    private int bestUnitPrice = Integer.MAX_VALUE;
    private int additionalUnitPrice = Integer.MAX_VALUE;

    public Slot getCheapestSlot() { return cheapestSlot; }
    public Slot getBestUnitSlot() { return bestUnitSlot; }
    public Slot getAdditionalUnitSlot() { return additionalUnitSlot; }
    public boolean isAuctionScreenActive() { return auctionScreenActive; }
    public boolean isHighlightThree() { return false; }

    @Subscribe
    public void onScoreBoardEvent(ScoreBoardEvent event) {
        List<Pair<ScoreboardEntry, Text>> lines = event.getList();
        if (lines.isEmpty()) {
            resetBalance();
            return;
        }

        for (Pair<ScoreboardEntry, Text> pair : lines) {
            String component = pair.getSecond().getString();
            if (component.contains("Монеты:")) {
                String[] splitted = component.split(":");
                if (splitted.length > 1) {
                    String digits = splitted[1].replaceAll("[^0-9]", "");
                    if (!digits.isEmpty()) {
                        try {
                            balance = Integer.parseInt(digits);
                            return;
                        } catch (NumberFormatException ignored) {
                            resetBalance();
                            return;
                        }
                    }
                }
                resetBalance();
                return;
            }
        }
    }

    public void analyze(ScreenHandler handler) {
        if (!isEnabled()) {
            auctionScreenActive = false;
            clearHighlights();
            return;
        }

        if (!isAuction(handler)) {
            auctionScreenActive = false;
            clearHighlights();
            return;
        }

        auctionScreenActive = true;
        cheapestSlot = null;
        bestUnitSlot = null;
        additionalUnitSlot = null;
        cheapestPrice = Integer.MAX_VALUE;
        bestUnitPrice = Integer.MAX_VALUE;
        additionalUnitPrice = Integer.MAX_VALUE;

        int slotCount = Math.min(handler.slots.size(), 44);
        for (int i = 0; i < slotCount; i++) {
            Slot slot = handler.getSlot(i);
            if (!slot.hasStack()) continue;

            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            int price = ItemUtil.getPrice(stack);
            if (price <= 0) continue;

            if (price < cheapestPrice) {
                cheapestPrice = price;
                cheapestSlot = slot;
            }

            int count = Math.max(1, stack.getCount());
            int unitPrice = price / count;

            if (unitPrice < bestUnitPrice || slot == bestUnitSlot) {
                if (slot != bestUnitSlot) {
                    additionalUnitSlot = bestUnitSlot;
                    additionalUnitPrice = bestUnitPrice;
                }
                bestUnitSlot = slot;
                bestUnitPrice = unitPrice;
            } else if (slot != bestUnitSlot && unitPrice < additionalUnitPrice) {
                additionalUnitSlot = slot;
                additionalUnitPrice = unitPrice;
            }
        }

        if (bestUnitSlot == additionalUnitSlot) {
            additionalUnitSlot = null;
        }
    }

    private void clearHighlights() {
        cheapestSlot = null;
        bestUnitSlot = null;
        additionalUnitSlot = null;
        cheapestPrice = Integer.MAX_VALUE;
        bestUnitPrice = Integer.MAX_VALUE;
        additionalUnitPrice = Integer.MAX_VALUE;
    }

    private boolean isAuction(ScreenHandler handler) {
        return handler != null
                && handler.slots.size() == 90
                && handler.getSlot(49).hasStack()
                && handler.getSlot(49).getStack().isOf(Items.NETHER_STAR);
    }

    public void renderCheapest(DrawContext context, Slot slot) {
        fillSlot(context, slot, 0xFF8C40FF);
    }

    public void renderBest(DrawContext context, Slot slot) {
        fillSlot(context, slot, 0xFF8CFFFF);
    }

    public void renderAdditional(DrawContext context, Slot slot) {
        fillSlot(context, slot, 0xFF8C40C4);
    }

    private void fillSlot(DrawContext context, Slot slot, int color) {
        context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color);
    }

    public void resetBalance() {
        balance = -1;
    }

    @Subscribe
    public void onKey(KeyBindEvent event) {
        if (event.getKey() == openAuctionBind.getKey() && openAuctionBind.getKey() != GLFW.GLFW_KEY_UNKNOWN) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null && mc.getNetworkHandler() != null) {
                ItemStack held = mc.player.getMainHandStack();
                if (!held.isEmpty()) {
                    String itemName = held.getName().getString();
                    mc.getNetworkHandler().sendChatCommand("ah search " + itemName);
                }
            }
        }
    }

    @Override
    public void onDisabled() {
        super.onDisabled();
        auctionScreenActive = false;
        clearHighlights();
        resetBalance();
    }
}
