package net.marewmod.quiver.compat;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.marewmod.quiver.recipe.QuiverSmithingRecipe;

import java.util.List;

public class QuiverSmithingDisplay extends BasicDisplay {

    public static final CategoryIdentifier<QuiverSmithingDisplay> ID =
        CategoryIdentifier.of("quiver", "smithing");

    public QuiverSmithingDisplay(QuiverSmithingRecipe recipe) {
        super(
            List.of(
                EntryIngredient.empty(),                                         // template slot (empty)
                EntryIngredients.ofIngredient(recipe.getIngredients().get(1)),  // base (leather quiver)
                EntryIngredients.ofIngredient(recipe.getIngredients().get(2))   // addition (material)
            ),
            List.of(
                EntryIngredients.of(recipe.getOutput(DynamicRegistryManager.EMPTY))
            )
        );
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return ID;
    }
}
