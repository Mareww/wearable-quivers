package net.marewmod.quiver;

import dev.emi.trinkets.api.SlotReference;
import net.minecraft.item.ItemStack;

/** Holds cross-mixin state for the single-arrow-per-shot consumption logic. */
public final class QuiverShotTracker {
    public static final ThreadLocal<ItemStack>    QUIVER   = new ThreadLocal<>();
    /** The exact Trinkets slot the quiver lives in — guarantees we modify the real slot, not a copy. */
    public static final ThreadLocal<SlotReference> SLOT_REF = new ThreadLocal<>();
    private QuiverShotTracker() {}
}
