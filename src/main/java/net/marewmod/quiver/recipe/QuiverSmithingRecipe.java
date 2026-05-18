package net.marewmod.quiver.recipe;

import com.google.gson.JsonObject;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import net.marewmod.quiver.QuiverMod;

import java.util.stream.Stream;

public class QuiverSmithingRecipe implements SmithingRecipe {

    public static final RecipeSerializer<QuiverSmithingRecipe> SERIALIZER =
        Registry.register(Registries.RECIPE_SERIALIZER,
            new Identifier(QuiverMod.MOD_ID, "quiver_smithing"),
            new Serializer());

    private final Identifier id;
    private final Ingredient base;
    private final Ingredient addition;
    private final ItemStack result;

    public QuiverSmithingRecipe(Identifier id, Ingredient base, Ingredient addition, ItemStack result) {
        this.id = id;
        this.base = base;
        this.addition = addition;
        this.result = result;
    }

    /** Template slot must be empty for this recipe — no template required or accepted. */
    @Override public boolean testTemplate(ItemStack stack) { return stack.isEmpty(); }
    @Override public boolean testBase(ItemStack stack)     { return this.base.test(stack); }
    @Override public boolean testAddition(ItemStack stack) { return this.addition.test(stack); }

    @Override
    public boolean matches(Inventory inventory, World world) {
        // slot 0 = template (ignored), slot 1 = base, slot 2 = addition
        return this.base.test(inventory.getStack(1)) && this.addition.test(inventory.getStack(2));
    }

    @Override
    public ItemStack craft(Inventory inventory, DynamicRegistryManager manager) {
        ItemStack out = this.result.copy();
        NbtCompound nbt = inventory.getStack(1).getNbt();
        if (nbt != null) out.setNbt(nbt.copy());
        return out;
    }

    @Override public ItemStack getOutput(DynamicRegistryManager manager) { return this.result; }
    @Override public Identifier getId() { return this.id; }
    @Override public RecipeSerializer<?> getSerializer() { return SERIALIZER; }

    @Override
    public boolean isEmpty() {
        return Stream.of(this.base, this.addition).anyMatch(Ingredient::isEmpty);
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        DefaultedList<Ingredient> list = DefaultedList.of();
        list.add(Ingredient.EMPTY); // no template required
        list.add(this.base);
        list.add(this.addition);
        return list;
    }

    public static class Serializer implements RecipeSerializer<QuiverSmithingRecipe> {
        @Override
        public QuiverSmithingRecipe read(Identifier id, JsonObject json) {
            Ingredient base     = Ingredient.fromJson(JsonHelper.getElement(json, "base"));
            Ingredient addition = Ingredient.fromJson(JsonHelper.getElement(json, "addition"));
            ItemStack  result   = ShapedRecipe.outputFromJson(JsonHelper.getObject(json, "result"));
            return new QuiverSmithingRecipe(id, base, addition, result);
        }

        @Override
        public QuiverSmithingRecipe read(Identifier id, PacketByteBuf buf) {
            return new QuiverSmithingRecipe(id,
                Ingredient.fromPacket(buf),
                Ingredient.fromPacket(buf),
                buf.readItemStack()
            );
        }

        @Override
        public void write(PacketByteBuf buf, QuiverSmithingRecipe r) {
            r.base.write(buf);
            r.addition.write(buf);
            buf.writeItemStack(r.result);
        }
    }
}
