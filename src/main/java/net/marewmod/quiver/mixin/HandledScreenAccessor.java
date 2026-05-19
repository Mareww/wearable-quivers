package net.marewmod.quiver.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    /** Left edge of the GUI background in screen pixels. */
    @Accessor("x")
    int getX();

    /** Top edge of the GUI background in screen pixels. */
    @Accessor("y")
    int getY();

    @Accessor("focusedSlot")
    net.minecraft.screen.slot.Slot getFocusedSlot();
}
