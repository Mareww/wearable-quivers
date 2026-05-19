package net.marewmod.quiver.enchantment;

import net.marewmod.quiver.item.QuiverItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class AutoRefillEnchantment extends Enchantment {

    public AutoRefillEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentTarget.VANISHABLE, new EquipmentSlot[0]);
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof QuiverItem;
    }

    @Override
    public int getMaxLevel() { return 1; }

    @Override
    public int getMinPower(int level) { return 25; }

    @Override
    public int getMaxPower(int level) { return 75; }

    /** Not available from enchanting table or librarian — only nether loot and fletcher trades. */
    @Override
    public boolean isAvailableForEnchantedBookOffer() { return false; }

    @Override
    public boolean isAvailableForRandomSelection() { return false; }
}
