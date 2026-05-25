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
    private final List<QuiverConfig.LootEntry> autoRefillList;
    private final List<QuiverConfig.LootEntry> quiverList;

    private final List<TextFieldWidget> autoRefillTableFields  = new ArrayList<>();
    private final List<TextFieldWidget> autoRefillChanceFields = new ArrayList<>();
    private final List<TextFieldWidget> quiverTableFields      = new ArrayList<>();
    private final List<TextFieldWidget> quiverChanceFields     = new ArrayList<>();

    private int autoRefillLabelY  = 0;
    private int autoRefillHeaderY = 0;
    private int quiverLabelY      = 0;
    private int quiverHeaderY     = 0;

    // Layout (set in init, read in render)
    private int startX  = 0;
    private int chanceX = 0;

    public QuiverLootTablesScreen(Screen parent) {
        super(Text.literal("Loot Table Settings"));
        this.parent = parent;
        QuiverConfig cfg = QuiverConfig.get();
        this.autoRefillList = new ArrayList<>(cfg.auto_refill_loot_entries);
        this.quiverList     = new ArrayList<>(cfg.quiver_loot_entries);
    }

    @Override
    protected void init() {
        autoRefillTableFields.clear();
        autoRefillChanceFields.clear();
        quiverTableFields.clear();
        quiverChanceFields.clear();

        int cx      = this.width / 2;
        int tableW  = 175, chanceW = 40, xW = 20, gap = 4;
        int totalW  = tableW + gap + chanceW + gap + xW;
        startX      = cx - totalW / 2;
        chanceX     = startX + tableW + gap;
        int xBtnX   = chanceX + chanceW + gap;
        int y       = 20;

        // ── Auto Refill Book section ─────────────────────────────────────────
        autoRefillLabelY  = y; y += 14;
        autoRefillHeaderY = y; y += 12;
        for (int i = 0; i < autoRefillList.size(); i++) {
            QuiverConfig.LootEntry entry = autoRefillList.get(i);

            TextFieldWidget tf = new TextFieldWidget(textRenderer, startX, y, tableW, 18, Text.empty());
            tf.setMaxLength(200);
            tf.setText(entry.table);
            addDrawableChild(tf);
            autoRefillTableFields.add(tf);

            TextFieldWidget cf = new TextFieldWidget(textRenderer, chanceX, y, chanceW, 18, Text.empty());
            cf.setMaxLength(5);
            cf.setText(String.valueOf(entry.chance));
            addDrawableChild(cf);
            autoRefillChanceFields.add(cf);

            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.literal("X"), btn -> {
                syncFields();
                autoRefillList.remove(idx);
                clearAndInit();
            }).dimensions(xBtnX, y, xW, 18).build());
            y += 22;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), btn -> {
            syncFields();
            autoRefillList.add(new QuiverConfig.LootEntry("", 6));
            clearAndInit();
        }).dimensions(startX, y, 24, 18).build());
        y += 30;

        // ── Quiver section ───────────────────────────────────────────────────
        quiverLabelY  = y; y += 14;
        quiverHeaderY = y; y += 12;
        for (int i = 0; i < quiverList.size(); i++) {
            QuiverConfig.LootEntry entry = quiverList.get(i);

            TextFieldWidget tf = new TextFieldWidget(textRenderer, startX, y, tableW, 18, Text.empty());
            tf.setMaxLength(200);
            tf.setText(entry.table);
            addDrawableChild(tf);
            quiverTableFields.add(tf);

            TextFieldWidget cf = new TextFieldWidget(textRenderer, chanceX, y, chanceW, 18, Text.empty());
            cf.setMaxLength(5);
            cf.setText(String.valueOf(entry.chance));
            addDrawableChild(cf);
            quiverChanceFields.add(cf);

            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.literal("X"), btn -> {
                syncFields();
                quiverList.remove(idx);
                clearAndInit();
            }).dimensions(xBtnX, y, xW, 18).build());
            y += 22;
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), btn -> {
            syncFields();
            quiverList.add(new QuiverConfig.LootEntry("", 12));
            clearAndInit();
        }).dimensions(startX, y, 24, 18).build());
        y += 34;

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> close())
            .dimensions(cx - 50, y, 100, 20).build());
    }

    private void syncFields() {
        for (int i = 0; i < autoRefillTableFields.size() && i < autoRefillList.size(); i++) {
            QuiverConfig.LootEntry e = autoRefillList.get(i);
            e.table = autoRefillTableFields.get(i).getText().trim();
            try { e.chance = Integer.parseInt(autoRefillChanceFields.get(i).getText().trim()); }
            catch (NumberFormatException ex) { e.chance = 6; }
        }
        for (int i = 0; i < quiverTableFields.size() && i < quiverList.size(); i++) {
            QuiverConfig.LootEntry e = quiverList.get(i);
            e.table = quiverTableFields.get(i).getText().trim();
            try { e.chance = Integer.parseInt(quiverChanceFields.get(i).getText().trim()); }
            catch (NumberFormatException ex) { e.chance = 12; }
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 6, 0xFFFFFF);

        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal("Auto Refill Book Chests:").formatted(Formatting.YELLOW),
            startX, autoRefillLabelY, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal("Table").formatted(Formatting.GRAY), startX, autoRefillHeaderY, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal("Chance %").formatted(Formatting.GRAY), chanceX, autoRefillHeaderY, 0xFFFFFF);

        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal("Quiver Chests:").formatted(Formatting.YELLOW),
            startX, quiverLabelY, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal("Table").formatted(Formatting.GRAY), startX, quiverHeaderY, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer,
            Text.literal("Chance %").formatted(Formatting.GRAY), chanceX, quiverHeaderY, 0xFFFFFF);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        syncFields();
        QuiverConfig cfg = QuiverConfig.get();
        cfg.auto_refill_loot_entries = new ArrayList<>(
            autoRefillList.stream().filter(e -> !e.table.isBlank()).toList());
        cfg.quiver_loot_entries = new ArrayList<>(
            quiverList.stream().filter(e -> !e.table.isBlank()).toList());
        QuiverConfig.save();
        this.client.setScreen(parent);
    }
}
