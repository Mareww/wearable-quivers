package net.marewmod.quiver;

import dev.emi.trinkets.api.SlotReference;
import net.marewmod.quiver.item.QuiverItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/** Holds cross-mixin state for the single-arrow-per-shot consumption logic. */
public final class QuiverShotTracker {
    public static final ThreadLocal<ItemStack>    QUIVER   = new ThreadLocal<>();
    /** The exact Trinkets slot the quiver lives in — guarantees we modify the real slot, not a copy. */
    public static final ThreadLocal<SlotReference> SLOT_REF = new ThreadLocal<>();
    private QuiverShotTracker() {}

    /**
     * Returns the total arrow count stored in the active quiver, or -1 if no quiver
     * supplied the current shot. Called by cross-mod compat (e.g. Advanced Enchantments)
     * to determine how many extra arrows can be fired.
     */
    public static int getAvailableCount() {
        ItemStack quiver = QUIVER.get();
        if (quiver == null) return -1;
        SlotReference slotRef = SLOT_REF.get();
        ItemStack live = (slotRef != null) ? slotRef.inventory().getStack(slotRef.index()) : quiver;
        return QuiverItem.getTotalCount(live);
    }

    /**
     * Consumes {@code count} extra arrows from the active quiver on behalf of a
     * multi-shot enchantment. Must be called before quiver's own TAIL clears the tracker.
     */
    public static void consumeExtra(PlayerEntity player, int count) {
        if (count <= 0) return;
        ItemStack quiver = QUIVER.get();
        if (quiver == null) return;
        SlotReference slotRef = SLOT_REF.get();
        if (slotRef != null) {
            ItemStack live = slotRef.inventory().getStack(slotRef.index());
            ItemStack copy = live.copy();
            for (int i = 0; i < count; i++) {
                QuiverItem.consumeOneArrow(copy);
            }
            QuiverItem.SUPPRESS_EQUIP_SOUND.add(player.getUuid());
            slotRef.inventory().setStack(slotRef.index(), copy);
        } else {
            for (int i = 0; i < count; i++) {
                QuiverItem.consumeOneArrow(quiver);
            }
        }
    }
}
