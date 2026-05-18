package net.marewmod.quiver.mixin;

import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public class GameModeSwitchMixin {

    @Inject(method = "changeGameMode", at = @At("TAIL"))
    private void syncTrinketsAfterGameModeChange(GameMode gameMode, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return; // mode didn't actually change
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        // Force Trinkets to sync its inventory to the client after a gamemode switch
        TrinketsApi.getTrinketComponent(player).ifPresent(comp ->
            comp.getInventory().values().forEach(groupMap ->
                groupMap.values().forEach(inv -> inv.markDirty())
            )
        );
        player.playerScreenHandler.sendContentUpdates();
    }
}
