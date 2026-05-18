package net.marewmod.quiver.compat;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import net.minecraft.recipe.RecipeType;
import net.marewmod.quiver.recipe.QuiverSmithingRecipe;

public class QuiverREIPlugin implements REIClientPlugin {

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new QuiverSmithingCategory());
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        registry.registerRecipeFiller(
            QuiverSmithingRecipe.class,
            RecipeType.SMITHING,
            QuiverSmithingDisplay::new
        );
    }
}
