package net.marewmod.quiver.compat;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.registry.DynamicRegistryManager;
import net.marewmod.quiver.recipe.QuiverSmithingRecipe;

public class QuiverSmithingEmiRecipe extends BasicEmiRecipe {

    public QuiverSmithingEmiRecipe(QuiverSmithingRecipe recipe) {
        super(VanillaEmiRecipeCategories.SMITHING, recipe.getId(), 116, 18);
        inputs.add(EmiIngredient.of(recipe.getIngredients().get(1))); // base quiver
        inputs.add(EmiIngredient.of(recipe.getIngredients().get(2))); // material
        outputs.add(EmiStack.of(recipe.getOutput(DynamicRegistryManager.EMPTY)));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.get(0), 0, 0);           // base
        widgets.addSlot(inputs.get(1), 28, 0);          // addition
        widgets.addSlot(outputs.get(0), 78, 0).recipeContext(this); // output
    }
}
