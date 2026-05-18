package net.marewmod.quiver.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.InfinityEnchantment;
import net.minecraft.text.MutableText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents EnchantmentDescriptions from adding its built-in Infinity description. */
@Mixin(targets = "net.darkhax.enchdesc.DescriptionManager", remap = false)
public class EnchDescMixin {

    @Inject(method = "get", at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressConservationDesc(Enchantment ench, CallbackInfoReturnable<MutableText> cir) {
        if (ench instanceof InfinityEnchantment) {
            cir.setReturnValue(net.minecraft.text.Text.empty()); // empty but non-null — safe for callers
        }
    }
}
