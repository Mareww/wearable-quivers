package net.marewmod.quiver.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class QuiverConfigScreen extends Screen {

    private final Screen parent;

    public QuiverConfigScreen(Screen parent) {
        super(Text.literal("Wearable Quivers Config"));
        this.parent = parent;
    }

    private CyclingButtonWidget<Boolean> toggle(int cx, int y, String label, boolean initial,
                                                  java.util.function.Consumer<Boolean> setter) {
        return CyclingButtonWidget.<Boolean>builder(v -> Text.literal(v ? "On" : "Off")
                .formatted(v ? Formatting.GREEN : Formatting.RED))
            .values(List.of(true, false))
            .initially(initial)
            .build(cx - 100, y, 200, 20, Text.literal(label), (btn, v) -> setter.accept(v));
    }

    @Override
    protected void init() {
        QuiverConfig cfg = QuiverConfig.get();
        int cx = this.width / 2;
        int y  = 30;

        // ── Rendering ──────────────────────────────────────────────────────
        addDrawableChild(toggle(cx, y, "Render Quiver",  cfg.render_quiver, v -> cfg.render_quiver = v)); y += 26;
        addDrawableChild(toggle(cx, y, "Body Strap",     cfg.show_strap,    v -> cfg.show_strap    = v)); y += 26;
        addDrawableChild(toggle(cx, y, "Wiggle Physics", cfg.enable_wiggle, v -> cfg.enable_wiggle = v)); y += 34;

        // ── Behaviour ──────────────────────────────────────────────────────
        addDrawableChild(toggle(cx, y, "Enchantment Glint", cfg.enchantment_glint, v -> cfg.enchantment_glint = v)); y += 26;

        // Auto Refill enchantment toggle — disable triggers a warning screen
        addDrawableChild(CyclingButtonWidget.<Boolean>builder(
            v -> Text.literal(v ? "True" : "False").formatted(v ? Formatting.GREEN : Formatting.RED))
            .values(List.of(true, false))
            .initially(cfg.auto_refill_enchantment)
            .build(cx - 100, y, 200, 20, Text.literal("Auto Refill Enchantment"),
                (btn, v) -> {
                    if (!v) {
                        cfg.auto_refill_enchantment = false; // tentative
                        client.setScreen(new ConfirmScreen(
                            confirmed -> {
                                if (!confirmed) cfg.auto_refill_enchantment = true;
                                client.setScreen(new QuiverConfigScreen(parent));
                            },
                            Text.literal("Disable Auto Refill?"),
                            Text.literal("The Auto Refill enchantment will be stripped from all\n"
                                + "quivers on the next /reload.\n"
                                + "It will also be removed from loot tables and\n"
                                + "villager trades on /reload."),
                            Text.literal("Disable"),
                            Text.literal("Cancel")
                        ));
                    } else {
                        cfg.auto_refill_enchantment = true;
                    }
                }));
        y += 26;

        addDrawableChild(ButtonWidget.builder(Text.literal("Edit Loot Tables"), btn -> {
            QuiverConfig.save();
            client.setScreen(new QuiverLootTablesScreen(this));
        }).dimensions(cx - 100, y, 200, 20).build());
        y += 34;

        // ── Position ───────────────────────────────────────────────────────
        y += 8;
        addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.literal(
                v.equals("leg") ? "Leg" : v.equals("force_back") ? "Force Back" : "Back"))
            .values(List.of("back", "leg", "force_back"))
            .initially(cfg.quiver_position)
            .build(cx - 100, y, 200, 20, Text.literal("Quiver Position"),
                (btn, v) -> cfg.quiver_position = v));
        y += 26;

        addDrawableChild(CyclingButtonWidget.<String>builder(v -> Text.literal(v.equals("left") ? "Left" : "Right"))
            .values(List.of("left", "right"))
            .initially(cfg.hip_side)
            .build(cx - 100, y, 200, 20, Text.literal("Leg Side"),
                (btn, v) -> cfg.hip_side = v));
        y += 34;

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
            .dimensions(cx - 50, y, 100, 20)
            .build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        QuiverConfig.save();
        this.client.setScreen(parent);
    }
}
