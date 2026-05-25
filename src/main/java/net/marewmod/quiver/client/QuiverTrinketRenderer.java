package net.marewmod.quiver.client;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.client.TrinketRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.marewmod.quiver.config.QuiverConfig;

@Environment(EnvType.CLIENT)
public class QuiverTrinketRenderer implements TrinketRenderer {

    /** Set before renderItem to switch to the strap-free hip model. */
    public static final ThreadLocal<Boolean> HIP_MODE     = ThreadLocal.withInitial(() -> false);
    /** Set before renderItem to switch to the thin baldric-strip model. */
    public static final ThreadLocal<Boolean> BALDRIC_MODE = ThreadLocal.withInitial(() -> false);

    // The baldric model strip is 4/16 = 0.25 blocks wide; SW scales it to 2 pixels wide.
    // 2 pixels = 2/16 = 0.125 blocks → SW = 0.125 / 0.25 = 0.5
    private static final float SW = 0.5f;

    @Override
    public void render(ItemStack stack, SlotReference slotReference, EntityModel<? extends LivingEntity> contextModel,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, LivingEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                       float headYaw, float headPitch) {

        // Hardcoded positions (tuned in-game)
        final float BACK_X = 0.148f, BACK_Y = 0.0f, BACK_Z = -0.1f, BACK_ROT = 30.0f;
        final float STRAP_X = 0.21f, STRAP_Y = 0.01f, STRAP_Z = -0.13f, STRAP_ROT = 14.53f;
        final float LEFT_LEG_X = 0.45f,  LEFT_LEG_Y  = -0.09f, LEFT_LEG_Z  = 0.07f;
        final float RIGHT_LEG_X = 0.059f, RIGHT_LEG_Y = -0.09f, RIGHT_LEG_Z = 0.07f;
        final float LEG_ROT = 132.1875f;

        QuiverConfig cfg = QuiverConfig.get();
        if (!cfg.render_quiver) return;
        boolean isHip  = cfg.quiver_position.equals("leg") ||
                         (!cfg.quiver_position.equals("force_back") && backOccupied(entity, slotReference));
        boolean isLeft = cfg.isHipLeft();

        if (contextModel instanceof BipedEntityModel<?> bm) {
            if (isHip) { if (isLeft) bm.leftLeg.rotate(matrices); else bm.rightLeg.rotate(matrices); }
            else bm.body.rotate(matrices);
        }

        matrices.push();

        if (isHip) {
            float hx = isLeft ? LEFT_LEG_X : RIGHT_LEG_X;
            float hy = isLeft ? LEFT_LEG_Y : RIGHT_LEG_Y;
            float hz = isLeft ? LEFT_LEG_Z : RIGHT_LEG_Z;
            matrices.translate(hx, hy, hz);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(LEG_ROT));
        } else {
            matrices.translate(BACK_X, BACK_Y, BACK_Z);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(BACK_ROT));
            if (cfg.enable_wiggle) {
                float sway = MathHelper.sin(limbAngle * 0.6662f) * limbDistance * 0.8f;
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sway));
            }
        }

        float scale = isHip ? 0.5f : 0.7f;
        matrices.scale(scale, scale, scale);
        matrices.translate(-0.5, -0.5, -0.5);

        if (isHip) HIP_MODE.set(true);
        renderItem(stack, matrices, vertexConsumers, entity, light);
        if (isHip) HIP_MODE.remove();

        matrices.pop();

        // Strap — fixed position, never wiggles, unaffected by config
        if (!isHip && cfg.show_strap) {
            matrices.push();
            matrices.translate(STRAP_X, STRAP_Y, STRAP_Z);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(STRAP_ROT));
            matrices.scale(0.7f, 0.7f, 0.7f);
            matrices.translate(-0.5f, -0.5f, -0.5f);
            BALDRIC_MODE.set(true);
            net.marewmod.quiver.item.QuiverItem.IN_TRINKET_RENDER.set(true);
            try { renderItem(stack, matrices, vertexConsumers, entity, light); }
            finally { net.marewmod.quiver.item.QuiverItem.IN_TRINKET_RENDER.set(false); }
            BALDRIC_MODE.remove();
            matrices.pop();
        }
    }

    /** Renders a strap from (x1,y1,z1) to (x2,y2,z2) in body-local space. */
    private void seg(ItemStack stack, MatrixStack m, VertexConsumerProvider vcp,
                     LivingEntity entity, int light,
                     float x1, float y1, float z1,
                     float x2, float y2, float z2) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 0.001f) return;
        float mx = (x1 + x2) * 0.5f, my = (y1 + y2) * 0.5f, mz = (z1 + z2) * 0.5f;

        m.push();
        m.translate(mx, my, mz);

        // Build rotation: align model +X axis with the (dx,dy,dz) direction.
        // First rotate in the XY plane, then tilt out of the XZ plane.
        float angZ = (float) Math.toDegrees(Math.atan2(dy, dx));   // XY plane rotation
        float angY = (float) Math.toDegrees(Math.atan2(-dz,        // Z tilt
                         (float) Math.sqrt(dx*dx + dy*dy)));

        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angZ));
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angY));
        m.scale(len, SW, SW);
        m.translate(-0.5f, -0.5f, -0.5f);
        renderItem(stack, m, vcp, entity, light);
        m.pop();
    }

    private void renderItem(ItemStack stack, MatrixStack m,
                             VertexConsumerProvider vcp, LivingEntity entity, int light) {
        MinecraftClient.getInstance().getItemRenderer().renderItem(
            entity, stack, ModelTransformationMode.NONE, false,
            m, vcp, entity.getWorld(), light, OverlayTexture.DEFAULT_UV, entity.getId()
        );
    }

    // Cache backOccupied result per entity per world tick to prevent per-frame flicker during sync.
    private static final java.util.HashMap<Integer, Boolean> BACK_CACHE = new java.util.HashMap<>();
    private static long BACK_CACHE_TICK = -1;

    /** Returns true if something back-rendering is equipped (stable across frames within the same tick). */
    private static boolean backOccupied(LivingEntity entity, SlotReference ownSlot) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        long tick = (client.world != null) ? client.world.getTime() : -1;
        if (tick != BACK_CACHE_TICK) { BACK_CACHE.clear(); BACK_CACHE_TICK = tick; }
        return BACK_CACHE.computeIfAbsent(entity.getId(), id -> computeBackOccupied(entity, ownSlot));
    }

    private static boolean computeBackOccupied(LivingEntity entity, SlotReference ownSlot) {
        // Vanilla chest slot: elytra and mod backpacks are not ArmorItem
        var chestArmor = entity.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST);
        if (!chestArmor.isEmpty() && !(chestArmor.getItem() instanceof net.minecraft.item.ArmorItem)) return true;

        // Trinket chest/back and chest/cape slots
        return dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(entity).map(comp -> {
            var chestGroup = comp.getInventory().get("chest");
            if (chestGroup == null) return false;
            for (String key : new String[]{ "back", "cape" }) {
                var inv = chestGroup.get(key);
                if (inv == null) continue;
                for (int i = 0; i < inv.size(); i++) {
                    if (inv == ownSlot.inventory() && i == ownSlot.index()) continue;
                    ItemStack s = inv.getStack(i);
                    if (!s.isEmpty() && !(s.getItem() instanceof net.marewmod.quiver.item.QuiverItem))
                        return true;
                }
            }
            return false;
        }).orElse(false);
    }
}
