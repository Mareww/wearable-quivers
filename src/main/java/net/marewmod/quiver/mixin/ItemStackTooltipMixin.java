package net.marewmod.quiver.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.marewmod.quiver.item.QuiverItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
public class ItemStackTooltipMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void quiver_removeTrinketSlotHint(PlayerEntity player, TooltipContext context,
                                               CallbackInfoReturnable<List<Text>> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (!(self.getItem() instanceof QuiverItem)) return;

        // Trinkets adds two kinds of entries:
        //   "trinkets.tooltip.*" - the "Fits in:" label line
        //   "trinkets.slot.*"    - the blue slot-name line (type.getTranslation())
        // Both must be removed.
        cir.getReturnValue().removeIf(text -> {
            if (text.getContent() instanceof TranslatableTextContent ttc
                    && ttc.getKey().startsWith("trinkets.")) {
                return true;
            }
            for (Text sibling : text.getSiblings()) {
                if (sibling.getContent() instanceof TranslatableTextContent stc
                        && stc.getKey().startsWith("trinkets.")) {
                    return true;
                }
            }
            return false;
        });
    }
}
