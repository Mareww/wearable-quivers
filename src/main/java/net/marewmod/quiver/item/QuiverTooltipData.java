package net.marewmod.quiver.item;

import net.minecraft.client.item.TooltipData;
import net.minecraft.item.ItemStack;

import java.util.List;

public record QuiverTooltipData(
    List<ItemStack> stacks,
    List<Integer>   counts,
    int selectedSlot,
    int totalStored,
    int totalCapacity
) implements TooltipData {}
