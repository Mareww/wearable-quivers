package net.marewmod.quiver.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.marewmod.quiver.QuiverMod;
import net.marewmod.quiver.item.QuiverItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class HandledScreenScrollMixin {

    @Shadow private MinecraftClient client;

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void quiver_interceptScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        // In-world: shift + scroll while holding bow → cycle quiver arrow type
        if (client.currentScreen == null && client.player != null && client.player.isSneaking()) {
            net.minecraft.entity.player.PlayerEntity p = client.player;
            net.minecraft.item.ItemStack mh = p.getMainHandStack();
            net.minecraft.item.ItemStack oh = p.getOffHandStack();
            boolean bow = mh.getItem() instanceof net.minecraft.item.BowItem
                       || mh.getItem() instanceof net.minecraft.item.CrossbowItem
                       || oh.getItem() instanceof net.minecraft.item.BowItem
                       || oh.getItem() instanceof net.minecraft.item.CrossbowItem;
            if (bow) {
                boolean hasMulti = dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(p)
                    .map(comp -> comp.getAllEquipped().stream().anyMatch(pair ->
                        pair.getRight().getItem() instanceof net.marewmod.quiver.item.QuiverItem
                        && net.marewmod.quiver.item.QuiverItem.getSlotCount(pair.getRight()) > 1))
                    .orElse(false);
                if (hasMulti) {
                    int direction = vertical < 0 ? 1 : -1;
                    var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                    buf.writeInt(direction);
                    buf.writeString(""); // no specific slot for in-world scroll
                    buf.writeString("");
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                        net.marewmod.quiver.QuiverMod.QUIVER_SCROLL_PACKET, buf);
                    ci.cancel();
                    return;
                }
            }
        }

        if (!(client.currentScreen instanceof HandledScreen)) return;
        HandledScreen<?> hs = (HandledScreen<?>) client.currentScreen;
        HandledScreenAccessor acc = (HandledScreenAccessor) hs;
        int sx = acc.getX(), sy = acc.getY();
        double mouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
        double mouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();

        boolean hasEquippedQuiver = client.player != null &&
            dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(client.player)
                .map(comp -> comp.getAllEquipped().stream()
                    .anyMatch(pair -> pair.getRight().getItem() instanceof QuiverItem))
                .orElse(false);

        Slot quiverSlot = null;
        for (Slot slot : hs.getScreenHandler().slots) {
            double rx = mouseX - sx, ry = mouseY - sy;
            if (rx < slot.x || rx >= slot.x + 16 || ry < slot.y || ry >= slot.y + 16) continue;
            if (slot.getStack().getItem() instanceof QuiverItem) { quiverSlot = slot; break; }
            if (hasEquippedQuiver && slot instanceof dev.emi.trinkets.TrinketSlot) { quiverSlot = slot; break; }
        }

        if (quiverSlot == null) return;

        int slotCount = QuiverItem.getSlotCount(quiverSlot.getStack());
        if (slotCount == 0 && hasEquippedQuiver && client.player != null) {
            slotCount = dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(client.player)
                .map(comp -> comp.getAllEquipped().stream()
                    .filter(pair -> pair.getRight().getItem() instanceof QuiverItem)
                    .mapToInt(pair -> QuiverItem.getSlotCount(pair.getRight()))
                    .findFirst().orElse(0))
                .orElse(0);
        }

        int direction = vertical < 0 ? 1 : -1;

        // Find which Trinkets slot is being scrolled by matching the slot's backing inventory
        String slotGroup = "";
        String slotName  = "";
        if (quiverSlot instanceof dev.emi.trinkets.TrinketSlot && client.player != null) {
            var compOpt = dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(client.player);
            if (compOpt.isPresent()) {
                for (var pair : compOpt.get().getAllEquipped()) {
                    if (!(pair.getRight().getItem() instanceof QuiverItem)) continue;
                    var ref = pair.getLeft();
                    if (quiverSlot.inventory == ref.inventory()) {
                        slotGroup = ref.inventory().getSlotType().getGroup();
                        slotName  = ref.inventory().getSlotType().getName();
                        break;
                    }
                }
            }
        }

        var buf = PacketByteBufs.create();
        buf.writeInt(direction);
        buf.writeString(slotGroup);
        buf.writeString(slotName);
        ClientPlayNetworking.send(QuiverMod.QUIVER_SCROLL_PACKET, buf);
        ci.cancel();
    }
}
