package net.marewmod.quiver.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class QuiverLootTablesScreen extends Screen {

    private final Screen parent;
    private final List<String> autoRefillList;
    private final List<String> quiverList;

    private final List<TextFieldWidget> autoRefillFields = new ArrayList<>();
    private final List<TextFieldWidget> quiverFields     = new ArrayList<>();

    private int autoRefillLabelY = 0;
    private int quiverLabelY     = 0;

    public QuiverLootTablesScreen(Screen parent) {
        super(Text.literal("Loot Table Settings"));
        this.parent = parent;
        QuiverConfig cfg = QuiverConfig.get();
        this.autoRefillList = new ArrayList<>(cfg.auto_refill_loot_tables);
        this.quiverList     = new ArrayList<>(cfg.quiver_loot_tables);
    }

    @Override
    protected void init() {
        autoRefillFields.clear();
        quiverFields.clear();

        int cx      = this.width / 2;
        int fieldW  = 220;
        int xBtn    = 20;
        int gap     = 4;
        int startX  = cx - (fieldW + gap + xBtn) / 2;
        int xBtnX   = startX + fieldW + gap;
        int y       = 20;

        // ── Auto Refill Book section ────────────────────────────────────────
        autoRefillLabelY = y;
        y += 14;
        for (int i = 0; i < autoRefillList.size(); i++) {
            TextFieldWidget field = new TextFieldWidget(textRenderer, startX, y, fieldW, 18, Text.empty());
            field.setMaxLength(200);
            field.setText(autoRefillList.get(i));
            addDrawableChild(field);
            autoRefillFields.add(field);

            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.literal("X"), btn -> {
                syncFields();
                autoRefillList.remove(idx);
                clearAndInit();
            }).dimensions(xBtnX, y, xBtn, 18).build());
            y += 22;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), btn -> {
            syncFields();
            autoRefillList.add("");
            clearAndInit();
        }).dimensions(startX, y, 24, 18).build());
        y += 30;

        // ── Quiver section ──────────────────────────────────────────────────
        quiverLabelY = y;
        y += 14;
        for (int i = 0; i < quiverList.size(); i++) {
            TextFieldWidget field = new TextFieldWidget(textRenderer, startX, y, fieldW, 18, Text.empty());
            field.setMaxLength(200);
            field.setText(quiverList.get(i));
            addDrawableChild(field);
            quiverFields.add(field);

            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.literal("X"), btn -> {
                syncFields();
                quiverList.remove(idx);
                clearAndInit();
            }).dimensions(xBtnX, y, xBtn, 18).build());
            y += 22;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), btn -> {
            syncFields();
            quiverList.add("");
            clearAndInit();
        }).dimensions(startX, y, 24, 18).build());
        y += 34;

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
            .dimensions(cx - 50, y, 100, 20).build());
    }

    private void syncFields() {
        for (int i = 0; i < autoRefillFields.size(); i++)
            if (i < autoRefillList.size()) autoRefillList.set(i, autoRefillFields.get(i).getText().trim());
        for (int i = 0; i < quiverFields.size(); i++)
            if (i < quiverList.size()) quiverList.set(i, quiverFields.get(i).getText().trim());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 6, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal("Auto Refill Book Chests:").formatted(Formatting.YELLOW),
            this.width / 2 - (220 + 4 + 20) / 2, autoRefillLabelY, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal("Quiver Chests:").formatted(Formatting.YELLOW),
            this.width / 2 - (220 + 4 + 20) / 2, quiverLabelY, 0xFFFFFF);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        syncFields();
        QuiverConfig cfg = QuiverConfig.get();
        cfg.auto_refill_loot_tables = new ArrayList<>(autoRefillList.stream().filter(s -> !s.isBlank()).toList());
        cfg.quiver_loot_tables      = new ArrayList<>(quiverList.stream().filter(s -> !s.isBlank()).toList());
        QuiverConfig.save();
        this.client.setScreen(parent);
    }
}
