package net.marewmod.quiver.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.marewmod.quiver.item.QuiverTooltipData;

import java.util.List;

@Environment(EnvType.CLIENT)
public class QuiverTooltipComponent implements TooltipComponent {

    private final List<ItemStack> stacks;
    private final List<Integer>   counts;
    private final int selectedSlot;
    private final int totalStored;
    private final int totalCapacity;

    public QuiverTooltipComponent(QuiverTooltipData data) {
        this.stacks        = data.stacks();
        this.counts        = data.counts();
        this.selectedSlot  = data.selectedSlot();
        this.totalStored   = data.totalStored();
        this.totalCapacity = data.totalCapacity();
    }

    private static final int ROW_H  = 20;
    private static final int BAR_H  = 2;
    private static final int GAP    = 3;

    @Override
    public int getHeight() {
        // one row per type + overall bar at bottom
        return stacks.size() * ROW_H + BAR_H + GAP * 2;
    }

    @Override
    public int getWidth(TextRenderer r) {
        // icon(16) + gap + count(max 3 digits) + gap + name + gap + bar(60)
        int maxName = stacks.stream()
            .mapToInt(s -> r.getWidth(s.getName().getString()))
            .max().orElse(0);
        return 16 + GAP + r.getWidth("320") + GAP + maxName + GAP + 60;
    }

    @Override
    public void drawItems(TextRenderer r, int x, int y, DrawContext ctx) {
        int barFullWidth = 60;

        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            int count       = counts.get(i);
            int rowY        = y + i * ROW_H;
            boolean sel     = i == selectedSlot;

            // subtle highlight for selected slot
            if (sel) ctx.fill(x - 1, rowY - 1, x + getWidth(r) + 1, rowY + 17, 0x33FFFFFF);

            // icon
            ctx.drawItem(stack, x, rowY);

            int textY = rowY + (16 - r.fontHeight) / 2;

            // count
            String cnt = String.valueOf(count);
            ctx.drawText(r, Text.literal(cnt), x + 16 + GAP, textY, 0xFFFFFF, true);

            // arrow type name (orange if selected)
            String name = stack.getName().getString();
            int nameX   = x + 16 + GAP + r.getWidth("320") + GAP;
            ctx.drawText(r, Text.literal(name), nameX, textY, sel ? 0xFFFFFFFF : 0xAAAAAA, true);

            // per-type proportion bar (shows how much of the 320 this type uses)
            int barX = nameX + r.getWidth(name) + GAP;
            int barY = rowY + (16 - BAR_H) / 2;
            ctx.fill(barX, barY, barX + barFullWidth, barY + BAR_H, 0xFF404040);
            int filled = totalCapacity > 0 ? (int)(barFullWidth * count / (float) totalCapacity) : 0;
            ctx.fill(barX, barY, barX + filled, barY + BAR_H, sel ? 0xFFFFFFFF : 0xFF888888);
        }

        // overall fill bar at bottom — color shifts green → yellow → red as it fills
        int barY   = y + stacks.size() * ROW_H + GAP;
        int totalW = getWidth(r);
        ctx.fill(x, barY, x + totalW, barY + BAR_H, 0xFF404040);
        float ratio = totalCapacity > 0 ? totalStored / (float) totalCapacity : 0f;
        int barColor;
        if (ratio <= 0.5f) {
            float t = ratio * 2f;                         // 0→1 as 0%→50%
            int rr = (int)(0x33 + t * (0xFF - 0x33));    // 0x33 → 0xFF
            int gg = 0xCC;
            int bb = (int)(0x33 * (1f - t));              // fades out
            barColor = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
        } else {
            float t = (ratio - 0.5f) * 2f;               // 0→1 as 50%→100%
            int rr = 0xFF;
            int gg = (int)(0xCC * (1f - t));              // fades to 0
            barColor = 0xFF000000 | (rr << 16) | (gg << 8);
        }
        int filled = (int)(totalW * ratio);
        ctx.fill(x, barY, x + filled, barY + BAR_H, barColor);

        // overall count text after the bar
        // (drawn below the bar as a caption)
    }
}
