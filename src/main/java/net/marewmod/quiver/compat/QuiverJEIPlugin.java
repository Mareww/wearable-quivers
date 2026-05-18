package net.marewmod.quiver.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.util.Identifier;
import net.marewmod.quiver.QuiverMod;
import net.marewmod.quiver.recipe.QuiverSmithingRecipe;

import java.util.List;

@JeiPlugin
public class QuiverJEIPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid() {
        return new Identifier(QuiverMod.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        RecipeManager manager = client.world.getRecipeManager();

        List<SmithingRecipe> recipes = manager
            .listAllOfType(net.minecraft.recipe.RecipeType.SMITHING)
            .stream()
            .filter(r -> r instanceof QuiverSmithingRecipe)
            .map(r -> (SmithingRecipe) r)
            .toList();

        registration.addRecipes(RecipeTypes.SMITHING, recipes);
    }
}
