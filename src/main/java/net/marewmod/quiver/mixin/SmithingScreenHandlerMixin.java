package net.marewmod.quiver.mixin;

import net.minecraft.recipe.SmithingRecipe;
import net.minecraft.screen.SmithingScreenHandler;
import net.marewmod.quiver.recipe.QuiverSmithingRecipe;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmithingScreenHandler.class)
public class SmithingScreenHandlerMixin {

    @Shadow @Nullable private SmithingRecipe currentRecipe;

    /** Skip consuming the template slot (slot 0) when crafting a quiver upgrade recipe. */
    @Inject(method = "decrementStack", at = @At("HEAD"), cancellable = true)
    private void skipTemplateConsumption(int slot, CallbackInfo ci) {
        if (slot == 0 && currentRecipe instanceof QuiverSmithingRecipe) {
            ci.cancel();
        }
    }
}
