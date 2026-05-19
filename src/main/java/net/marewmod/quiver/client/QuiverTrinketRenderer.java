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
        boolean isHip  = slotReference.inventory().getSlotType().getName().equals("quiver") && slotReference.inventory().getSlotType().getGroup().equals("legs");
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

    // ── Baldric: 3 strap segments in body-local space ──────────────────────────

    private void renderBaldric(ItemStack stack, MatrixStack m,
                                VertexConsumerProvider vcp, LivingEntity entity, int light,
                                QuiverConfig cfg) {
        BALDRIC_MODE.set(true);

        // ── Anatomy (body-local coords: +X=player LEFT, +Y=DOWN, +Z=BEHIND player) ──
        // Player body: ±0.25 wide(X), Y=0(shoulder top) to Y=0.75(waist), Z=±0.125(depth)
        //
        // Quiver back-left edge ≈ -0.1f + 0.09  (model half-width 0.125 × scale 0.7)
        // Quiver upper area    ≈ 0.35f − 0.26   (model top Y=16 maps to body Y≈back.y−0.35)

        // Body: Y=2..13, scale=0.7 → center Y=(7.5/16)*0.7+back.y
        // Rim top: Y=15/16=0.9375 → body-local ≈ back.y − (0.9375−0.5)*0.7 ≈ back.y − 0.306
        float sx = -0.1f + 0.09f;   // quiver left edge (model X=10 at this scale)
        float sy = 0.35f - 0.28f;   // upper body just below rim top
        float bz = 0.13f;           // back surface Z

        // Fixed anatomical endpoints derived from player model
        float shoulderX =  0.25f;  // left shoulder = body left edge
        float shoulderY =  0.02f;  // just below shoulder top
        float frontZ    = -0.14f;  // chest front face Z
        float hipX      = -0.20f;  // right hip
        float hipY      =  0.68f;  // waist level

        // Segment 1: back diagonal — quiver upper area → left shoulder (back face)
        seg(stack, m, vcp, entity, light, sx, sy, bz, shoulderX, shoulderY, bz);

        // Segment 2: shoulder wrap — left shoulder wraps over from back to front
        seg(stack, m, vcp, entity, light, shoulderX, shoulderY, bz, shoulderX, shoulderY, frontZ);

        // Segment 3: chest diagonal — left shoulder (front) → right hip (front)
        seg(stack, m, vcp, entity, light, shoulderX, shoulderY, frontZ, hipX, hipY, frontZ);

        BALDRIC_MODE.remove();
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

    /**
     * Renders one strap segment.
     * The baldric model is a strip 1.0 long × 0.25 wide × 0.25 thick (in [0,1] block space),
     * centered at origin after translate(-0.5, -0.5, -0.5).
     * We scale X to the desired length and Y/Z to a thin strap width.
     */
    private void renderStrap(ItemStack stack, MatrixStack m,
                              VertexConsumerProvider vcp, LivingEntity entity, int light,
                              float mx, float my, float mz,
                              RotationAxis rotAxis, float rotAngle,
                              float length) {
        m.push();
        m.translate(mx, my, mz);
        m.multiply(rotAxis.rotationDegrees(rotAngle));
        m.scale(length, SW, SW);
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
}
