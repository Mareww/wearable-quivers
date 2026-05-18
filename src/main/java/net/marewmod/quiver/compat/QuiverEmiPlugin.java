package net.marewmod.quiver.compat;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.recipe.RecipeType;
import net.marewmod.quiver.recipe.QuiverSmithingRecipe;

public class QuiverEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        if (MinecraftClient.getInstance().world == null) return;

        MinecraftClient.getInstance().world.getRecipeManager()
            .listAllOfType(RecipeType.SMITHING)
            .stream()
            .filter(r -> r instanceof QuiverSmithingRecipe)
            .map(r -> (QuiverSmithingRecipe) r)
            .forEach(recipe -> registry.addRecipe(new QuiverSmithingEmiRecipe(recipe)));
    }
}
