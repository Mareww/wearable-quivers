package net.marewmod.quiver.item;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.item.TooltipData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import dev.emi.trinkets.api.TrinketItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuiverItem extends Item implements Trinket, DyeableItem {

    /** Total arrow capacity regardless of how many different types are stored. Enchantments can raise this later. */
    public static final int TOTAL_CAPACITY = 64;

    /** Set during right-click equip to steer Trinkets into the preferred slot group. */
    public static final ThreadLocal<String> EQUIP_PREFERRED_GROUP = ThreadLocal.withInitial(() -> null);



    public static final String SLOTS_KEY = "ArrowSlots";
    private static final String ID_KEY    = "Id";
    private static final String COUNT_KEY = "Count";
    private static final String SEL_KEY   = "SelectedSlot";

    public QuiverItem(Settings settings) {
        super(settings);
        TrinketsApi.registerTrinket(this, this);
    }

    // ── NBT helpers ──────────────────────────────────────────────────────────

    /** Returns the live NbtList (modifications persist in the stack). */
    public static NbtList getSlots(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (!nbt.contains(SLOTS_KEY, NbtElement.LIST_TYPE))
            nbt.put(SLOTS_KEY, new NbtList());
        return nbt.getList(SLOTS_KEY, NbtElement.COMPOUND_TYPE);
    }

    public static int getSlotCount(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? 0 : nbt.getList(SLOTS_KEY, NbtElement.COMPOUND_TYPE).size();
    }

    public static int getTotalCount(ItemStack stack) {
        NbtList slots = getSlots(stack);
        int total = 0;
        for (int i = 0; i < slots.size(); i++) total += slots.getCompound(i).getInt(COUNT_KEY);
        return total;
    }

    @Override
    public boolean canEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {
        // Steer right-click equip to the preferred group when set
        String preferred = EQUIP_PREFERRED_GROUP.get();
        if (preferred != null && !slot.inventory().getSlotType().getGroup().equals(preferred))
            return false;
        // Only one quiver at a time
        return TrinketsApi.getTrinketComponent(entity).map(comp -> {
            for (var pair : comp.getAllEquipped()) {
                if (pair.getRight().getItem() instanceof QuiverItem) return false;
            }
            return true;
        }).orElse(true);
    }

    @Override
    public void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity) {}

    @Override
    public void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity) {}
    // Sound is played directly in use() so it only fires on explicit right-click equip,
    // never on scroll/shoot NBT changes or Trinkets component syncs.

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        // Try chest/back first (natural exclusion with backpack); fall back to legs/quiver
        EQUIP_PREFERRED_GROUP.set("chest");
        boolean equipped = TrinketItem.equipItem(user, stack);
        EQUIP_PREFERRED_GROUP.remove();
        if (!equipped) equipped = TrinketItem.equipItem(user, stack);
        if (equipped && !world.isClient())
            user.playSound(net.minecraft.sound.SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.0f);
        return equipped ? TypedActionResult.success(stack) : TypedActionResult.fail(stack);
    }

    /** Override in subclasses to set a different capacity. */
    public int capacity() {
        return TOTAL_CAPACITY;
    }

    public static int getMaxCapacity(ItemStack stack) {
        if (stack.getItem() instanceof QuiverItem q) return q.capacity();
        return TOTAL_CAPACITY;
    }

    public static int getSelectedSlot(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) return 0;
        int n = getSlotCount(stack);
        return n == 0 ? 0 : MathHelper.clamp(nbt.getInt(SEL_KEY), 0, n - 1);
    }

    public static void setSelectedSlot(ItemStack stack, int slot) {
        stack.getOrCreateNbt().putInt(SEL_KEY, slot);
    }

    // ── Insert / extract ─────────────────────────────────────────────────────

    private static boolean sameArrowType(NbtCompound slot, ItemStack arrows) {
        String id = Registries.ITEM.getId(arrows.getItem()).toString();
        if (!slot.getString(ID_KEY).equals(id)) return false;
        NbtCompound slotTag   = slot.contains("Tag", NbtElement.COMPOUND_TYPE) ? slot.getCompound("Tag") : null;
        NbtCompound arrowTag  = arrows.getNbt();
        if (slotTag == null && arrowTag == null) return true;
        if (slotTag == null || arrowTag == null) return false;
        return slotTag.equals(arrowTag);
    }

    public static int insertPublic(ItemStack quiver, ItemStack arrows) {
        return insert(quiver, arrows);
    }

    /** Inserts as many arrows as possible up to the total capacity. Returns the number actually inserted. */
    private static int insert(ItemStack quiver, ItemStack arrows) {
        if (!(arrows.getItem() instanceof ArrowItem)) return 0;
        String arrowId = Registries.ITEM.getId(arrows.getItem()).toString();
        NbtList slots  = getSlots(quiver);
        int space      = getMaxCapacity(quiver) - getTotalCount(quiver);
        if (space <= 0) return 0;
        int add = Math.min(space, arrows.getCount());

        // fill an existing slot of the same type (including NBT)
        for (int i = 0; i < slots.size(); i++) {
            NbtCompound e = slots.getCompound(i);
            if (!sameArrowType(e, arrows)) continue;
            e.putInt(COUNT_KEY, e.getInt(COUNT_KEY) + add);
            return add;
        }

        // open a new slot
        NbtCompound e = new NbtCompound();
        e.putString(ID_KEY, arrowId);
        e.putInt(COUNT_KEY, add);
        NbtCompound itemNbt = arrows.getNbt();
        if (itemNbt != null) e.put("Tag", itemNbt.copy());
        slots.add(e);
        return add;
    }

    /** Removes the selected slot and returns its contents. */
    private static final int EXTRACT_SIZE = 64;

    /** Takes up to 64 arrows from the selected slot. Removes the slot only when it empties. */
    private static ItemStack extract(ItemStack quiver) {
        NbtList slots = getSlots(quiver);
        if (slots.isEmpty()) return ItemStack.EMPTY;

        int sel = getSelectedSlot(quiver);
        NbtCompound e = slots.getCompound(sel);
        String arrowId = e.getString(ID_KEY);
        int stored     = e.getInt(COUNT_KEY);

        Identifier id = Identifier.tryParse(arrowId);
        if (id == null) { slots.remove(sel); return ItemStack.EMPTY; }
        Item item = Registries.ITEM.get(id);
        if (!(item instanceof ArrowItem)) { slots.remove(sel); return ItemStack.EMPTY; }

        int take = Math.min(stored, EXTRACT_SIZE);
        int remaining = stored - take;
        if (remaining == 0) {
            slots.remove(sel);
            int newSize = slots.size();
            if (newSize > 0 && sel >= newSize) setSelectedSlot(quiver, newSize - 1);
        } else {
            e.putInt(COUNT_KEY, remaining);
        }
        ItemStack out = new ItemStack(item, take);
        if (e.contains("Tag", NbtElement.COMPOUND_TYPE)) out.setNbt(e.getCompound("Tag").copy());
        return out;
    }

    /**
     * Returns a display name for an arrow slot entry, appending the trail color name
     * in brackets if the Advanced Fletching Table mod stored one in the Tag NBT.
     */
    public static String getArrowDisplayName(NbtCompound slotEntry) {
        Identifier id = Identifier.tryParse(slotEntry.getString(ID_KEY));
        if (id == null) return "Unknown";
        Item item = Registries.ITEM.get(id);
        ItemStack display = new ItemStack(item, 1);
        if (slotEntry.contains("Tag", NbtElement.COMPOUND_TYPE))
            display.setNbt(slotEntry.getCompound("Tag").copy());
        String name = display.getName().getString();
        // Advanced Fletching Table stores trail color name under "TrailColorName" in item NBT
        NbtCompound tag = display.getNbt();
        if (tag != null && tag.contains("TrailColorName")) {
            String colorName = tag.getString("TrailColorName");
            if (!colorName.isEmpty()) {
                colorName = Character.toUpperCase(colorName.charAt(0)) + colorName.substring(1);
                name = name + " [" + colorName + "]";
            }
        }
        return name;
    }

    /** Returns the arrow ItemStack at the given slot index (with its stored count). */
    public static ItemStack getArrowStack(ItemStack quiver, int index) {
        NbtList slots = getSlots(quiver);
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        NbtCompound e = slots.getCompound(index);
        Identifier id = Identifier.tryParse(e.getString(ID_KEY));
        if (id == null) return ItemStack.EMPTY;
        Item item = Registries.ITEM.get(id);
        if (!(item instanceof ArrowItem)) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item, e.getInt(COUNT_KEY));
        if (e.contains("Tag", NbtElement.COMPOUND_TYPE)) stack.setNbt(e.getCompound("Tag").copy());
        return stack;
    }

    /** Peeks at the next arrow WITHOUT consuming. Used by the shooting mixin. */
    public static ItemStack peekNextArrow(ItemStack quiver) {
        NbtList slots = getSlots(quiver);
        if (slots.isEmpty()) return ItemStack.EMPTY;

        int sel = Math.min(getSelectedSlot(quiver), slots.size() - 1);
        for (int attempt = 0; attempt < slots.size(); attempt++) {
            int idx = (sel + attempt) % slots.size();
            NbtCompound e = slots.getCompound(idx);
            if (e.getInt(COUNT_KEY) <= 0) continue;
            Identifier id = Identifier.tryParse(e.getString(ID_KEY));
            if (id == null) continue;
            Item item = Registries.ITEM.get(id);
            if (!(item instanceof ArrowItem)) continue;
            ItemStack peeked = new ItemStack(item, 1);
            if (e.contains("Tag", NbtElement.COMPOUND_TYPE)) peeked.setNbt(e.getCompound("Tag").copy());
            return peeked;
        }
        return ItemStack.EMPTY;
    }

    // ── Bundle-style inventory interaction ───────────────────────────────────

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot,
                              ClickType clickType, PlayerEntity player,
                              StackReference cursorStackReference) {
        // RIGHT click in both survival and creative inventory screens
        if (clickType != ClickType.RIGHT) return false;

        if (otherStack.isEmpty()) {
            ItemStack out = extract(stack);
            if (out.isEmpty()) return false;
            cursorStackReference.set(out);
        } else if (otherStack.getItem() instanceof ArrowItem) {
            int inserted = insert(stack, otherStack);
            if (inserted == 0) return false;
            otherStack.decrement(inserted);
        } else {
            return false;
        }

        // Write the modified stack back to the slot
        slot.setStack(stack);
        if (!player.getWorld().isClient()) {
            // Also force-write directly to the Trinkets inventory so creative mode
            // wrapper slots don't discard the NBT changes on mode switch
            TrinketsApi.getTrinketComponent(player).ifPresent(comp ->
                comp.getInventory().values().forEach(groupMap ->
                    groupMap.values().forEach(inv -> {
                        for (int j = 0; j < inv.size(); j++) {
                            if (inv.getStack(j).getItem() instanceof QuiverItem) {
                                inv.setStack(j, stack);
                                inv.markDirty();
                                return;
                            }
                        }
                    })
                )
            );
            player.playerScreenHandler.sendContentUpdates();
        }
        return true;
    }

    /**
     * Called by PlayerEntityMixin when a bow/crossbow fires and the player has
     * no arrows in their inventory. Removes 1 arrow from the selected slot and
     * returns it as a detached stack. The bow's normal consumption code operates
     * on this copy, so nothing in the quiver gets double-removed.
     */
    public static ItemStack consumeOneArrow(ItemStack quiver) {
        NbtList slots = getSlots(quiver);
        if (slots.isEmpty()) return ItemStack.EMPTY;

        int sel = Math.min(getSelectedSlot(quiver), slots.size() - 1);
        // try selected slot first, then others
        for (int attempt = 0; attempt < slots.size(); attempt++) {
            int idx = (sel + attempt) % slots.size();
            NbtCompound e = slots.getCompound(idx);
            int count = e.getInt(COUNT_KEY);
            if (count <= 0) continue;

            Identifier id = Identifier.tryParse(e.getString(ID_KEY));
            if (id == null) continue;
            Item item = Registries.ITEM.get(id);
            if (!(item instanceof ArrowItem)) continue;

            e.putInt(COUNT_KEY, count - 1);
            if (count - 1 == 0) {
                slots.remove(idx);
                int newSize = slots.size();
                if (newSize > 0 && sel >= newSize) setSelectedSlot(quiver, newSize - 1);
            }
            ItemStack consumed = new ItemStack(item, 1);
            if (e.contains("Tag", NbtElement.COMPOUND_TYPE)) consumed.setNbt(e.getCompound("Tag").copy());
            return consumed;
        }
        return ItemStack.EMPTY;
    }

    // ── Tooltip ──────────────────────────────────────────────────────────────

    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        NbtList slots = getSlots(stack);
        if (slots.isEmpty()) return Optional.empty();

        List<ItemStack> stacks = new ArrayList<>();
        List<Integer>   counts = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            NbtCompound e  = slots.getCompound(i);
            Identifier  id = Identifier.tryParse(e.getString(ID_KEY));
            if (id == null) continue;
            Item item = Registries.ITEM.get(id);
            ItemStack display = new ItemStack(item);
            if (e.contains("Tag", NbtElement.COMPOUND_TYPE)) display.setNbt(e.getCompound("Tag").copy());
            stacks.add(display);
            counts.add(e.getInt(COUNT_KEY));
        }
        int total = 0;
        for (int c : counts) total += c;
        return Optional.of(new QuiverTooltipData(stacks, counts, getSelectedSlot(stack),
                total, getMaxCapacity(stack)));
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        int total = getTotalCount(stack);
        int max   = getMaxCapacity(stack);
        if (total > 0) {
            tooltip.add(Text.literal(total + " / " + max + " arrows").formatted(Formatting.GRAY));
        } else {
            tooltip.add(Text.literal("Empty  (capacity: " + max + ")").formatted(Formatting.GRAY));
        }
        tooltip.add(Text.literal("Right-click quiver in inventory to insert/take arrows").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.literal("Scroll over quiver to choose which type to take").formatted(Formatting.DARK_GRAY));
    }
}
