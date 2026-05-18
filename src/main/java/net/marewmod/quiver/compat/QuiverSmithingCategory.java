package net.marewmod.quiver.compat;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class QuiverSmithingCategory implements DisplayCategory<QuiverSmithingDisplay> {

    @Override
    public CategoryIdentifier<QuiverSmithingDisplay> getCategoryIdentifier() {
        return QuiverSmithingDisplay.ID;
    }

    @Override
    public Text getTitle() {
        return Text.translatable("category.quiver.smithing");
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(new ItemStack(Blocks.SMITHING_TABLE));
    }

    @Override
    public List<Widget> setupDisplay(QuiverSmithingDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        int startX = bounds.x + 5;
        int midY = bounds.getCenterY() - 9;

        widgets.add(Widgets.createRecipeBase(bounds));

        // Template slot (empty — no template required)
        widgets.add(Widgets.createSlot(new Point(startX, midY))
            .entries(display.getInputEntries().get(0))
            .disableBackground());

        // Base slot (leather quiver)
        widgets.add(Widgets.createSlot(new Point(startX + 22, midY))
            .entries(display.getInputEntries().get(1)));

        // Addition slot (material)
        widgets.add(Widgets.createSlot(new Point(startX + 44, midY))
            .entries(display.getInputEntries().get(2)));

        // Arrow
        widgets.add(Widgets.createArrow(new Point(startX + 66, midY - 1)));

        // Output slot
        widgets.add(Widgets.createSlot(new Point(startX + 96, midY))
            .entries(display.getOutputEntries().get(0))
            .disableBackground());

        return widgets;
    }

    @Override
    public int getDisplayHeight() {
        return 40;
    }
}
