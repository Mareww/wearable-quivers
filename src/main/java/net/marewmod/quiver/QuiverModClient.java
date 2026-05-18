package net.marewmod.quiver;

import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.item.DyeableItem;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.marewmod.quiver.client.QuiverTooltipComponent;
import net.marewmod.quiver.client.QuiverTrinketRenderer;
import net.marewmod.quiver.config.QuiverConfig;
import net.marewmod.quiver.item.ModItems;
import net.marewmod.quiver.item.QuiverItem;
import net.marewmod.quiver.item.QuiverTooltipData;
import net.marewmod.quiver.mixin.HandledScreenAccessor;

@Environment(EnvType.CLIENT)
public class QuiverModClient implements ClientModInitializer {


    // Animated x offset: 0 = no prev arrow (first slot), 14 = prev arrow visible
    private static float slotXOffset = 0f;
    // Horizontal slide for the main arrow: positive = slides in from right, negative = from left
    private static float arrowXOffset = 0f;
    private static int lastArrowSel = -1;

    // Slot icon animation state
    private static net.minecraft.client.texture.NativeImageBackedTexture slotAnimTex;
    private static net.minecraft.client.texture.NativeImage slotFrame0; // quiver icon
    private static net.minecraft.client.texture.NativeImage slotFrame1; // back slot icon
    private static int slotAnimTick = 0;
    private static final net.minecraft.util.Identifier SLOT_ANIM_ID =
        new net.minecraft.util.Identifier("trinkets", "textures/gui/slots/back.png");

    @Override
    public void onInitializeClient() {
        QuiverConfig.load();


        // Load both slot icon frames after resources load and combine them into an animated texture
        net.fabricmc.fabric.api.resource.ResourceManagerHelper
            .get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES)
            .registerReloadListener(new net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener() {
                @Override
                public net.minecraft.util.Identifier getFabricId() {
                    return new net.minecraft.util.Identifier("quiver", "slot_icon_anim");
                }
                @Override
                public void reload(net.minecraft.resource.ResourceManager manager) {
                    try {
                        if (slotFrame0 != null) { slotFrame0.close(); slotFrame0 = null; }
                        if (slotFrame1 != null) { slotFrame1.close(); slotFrame1 = null; }
                        slotFrame0 = net.minecraft.client.texture.NativeImage.read(
                            manager.getResource(new net.minecraft.util.Identifier("quiver", "textures/gui/slots/quiver_slot.png")).get().getInputStream());
                        slotFrame1 = net.minecraft.client.texture.NativeImage.read(
                            manager.getResource(new net.minecraft.util.Identifier("trinkets", "textures/gui/slots/back.png")).get().getInputStream());
                        // Create texture sized to frame0
                        int w = slotFrame0.getWidth(), h = slotFrame0.getHeight();
                        net.minecraft.client.texture.NativeImage img =
                            new net.minecraft.client.texture.NativeImage(w, h, false);
                        for (int y = 0; y < h; y++)
                            for (int x = 0; x < w; x++)
                                img.setColor(x, y, slotFrame0.getColor(x, y));
                        if (slotAnimTex != null) slotAnimTex.close();
                        slotAnimTex = new net.minecraft.client.texture.NativeImageBackedTexture(img);
                        MinecraftClient.getInstance().getTextureManager()
                            .registerTexture(SLOT_ANIM_ID, slotAnimTex);
                        slotAnimTick = 0;
                    } catch (Exception e) {
                        QuiverMod.LOGGER.warn("[Quiver] Could not load slot icon animation frames: {}", e.getMessage());
                    }
                }
            });

        // Swap between frame0 (quiver) and frame1 (back slot) every 20 ticks (1 second)
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (slotAnimTex == null || slotFrame0 == null || slotFrame1 == null) return;
            slotAnimTick++;
            if (slotAnimTick % 20 != 0) return;
            // frame 1 (back icon) shows first, then frame 0 (quiver) — matches show=1 wait 1s show=0
            boolean showBack = (slotAnimTick / 20) % 2 == 0;
            net.minecraft.client.texture.NativeImage src = showBack ? slotFrame1 : slotFrame0;
            net.minecraft.client.texture.NativeImage dst = slotAnimTex.getImage();
            if (dst == null) return;
            int w = dst.getWidth(), h = dst.getHeight();
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    dst.setColor(x, y, src.getColor(x, y));
            slotAnimTex.upload();
        });

        // Server → client: directly update SelectedSlot on the equipped quiver
        ClientPlayNetworking.registerGlobalReceiver(
            QuiverMod.QUIVER_SYNC_PACKET, (client, handler, buf, responseSender) -> {
                int selectedSlot = buf.readInt();
                client.execute(() -> {
                    if (client.player == null) return;
                });
            });

        // Arrow HUD: show selected arrow slot to the right of the hotbar when holding a bow and a quiver is equipped
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((context, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) return;
            net.minecraft.entity.player.PlayerEntity player = mc.player;

            // Only show while holding a bow or crossbow
            net.minecraft.item.ItemStack main = player.getMainHandStack();
            net.minecraft.item.ItemStack off  = player.getOffHandStack();
            boolean holdingBow = main.getItem() instanceof net.minecraft.item.BowItem
                              || main.getItem() instanceof net.minecraft.item.CrossbowItem
                              || off.getItem()  instanceof net.minecraft.item.BowItem
                              || off.getItem()  instanceof net.minecraft.item.CrossbowItem;
            if (!holdingBow) return;

            // Find equipped quiver first (enables prev/next arrows and slide animation)
            net.minecraft.item.ItemStack quiver = net.minecraft.item.ItemStack.EMPTY;
            var comp = dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(player);
            if (comp.isPresent()) {
                for (var pair : comp.get().getAllEquipped()) {
                    if (pair.getRight().getItem() instanceof QuiverItem) {
                        quiver = pair.getRight();
                        break;
                    }
                }
            }
            boolean isEquipped = !quiver.isEmpty();

            // Try quiver first; fall back to inventory arrows if quiver is empty/missing
            net.minecraft.item.ItemStack displayArrow = net.minecraft.item.ItemStack.EMPTY;
            if (!quiver.isEmpty()) {
                net.minecraft.item.ItemStack peek = QuiverItem.peekNextArrow(quiver);
                if (!peek.isEmpty()) {
                    int sel = QuiverItem.getSelectedSlot(quiver);
                    net.minecraft.nbt.NbtList arrowSlots = QuiverItem.getSlots(quiver);
                    int arrowCount = sel < arrowSlots.size() ? arrowSlots.getCompound(sel).getInt("Count") : 1;
                    displayArrow = new net.minecraft.item.ItemStack(peek.getItem(), arrowCount);
                    if (peek.getNbt() != null) displayArrow.setNbt(peek.getNbt().copy());
                }
            }
            if (displayArrow.isEmpty()) {
                // No quiver arrows — sum all stacks of the same arrow type in inventory
                net.minecraft.item.Item firstArrowType = null;
                int totalCount = 0;
                net.minecraft.item.ItemStack firstStack = net.minecraft.item.ItemStack.EMPTY;
                for (int i = 0; i < player.getInventory().size(); i++) {
                    net.minecraft.item.ItemStack s = player.getInventory().getStack(i);
                    if (!(s.getItem() instanceof net.minecraft.item.ArrowItem)) continue;
                    if (firstArrowType == null) { firstArrowType = s.getItem(); firstStack = s; }
                    if (s.getItem() == firstArrowType) totalCount += s.getCount();
                }
                if (firstArrowType != null) {
                    displayArrow = new net.minecraft.item.ItemStack(firstArrowType, totalCount);
                    if (firstStack.getNbt() != null) displayArrow.setNbt(firstStack.getNbt().copy());
                }
            }
            if (displayArrow.isEmpty()) return;

            int sw = mc.getWindow().getScaledWidth();
            int sh = mc.getWindow().getScaledHeight();

            net.minecraft.nbt.NbtList arrowSlots2 = QuiverItem.getSlots(quiver);
            int numTypes = arrowSlots2.size();
            int sel2 = QuiverItem.getSelectedSlot(quiver);

            // No wrap: stop at first and last
            boolean hasPrev = isEquipped && numTypes > 1 && sel2 > 0;
            boolean hasNext = isEquipped && numTypes > 1 && sel2 < numTypes - 1;
            int prevIdx = sel2 - 1;
            int nextIdx = sel2 + 1;

            // Smooth slide: offset 0 = no prev, offset 14 = prev visible
            float targetOffset = hasPrev ? 14f : 0f;
            slotXOffset += (targetOffset - slotXOffset) * 0.2f;
            if (Math.abs(targetOffset - slotXOffset) < 0.5f) slotXOffset = targetOffset;

            int baseX = sw / 2 + 91 + 4;
            int x = baseX + (int)slotXOffset;
            int y = sh - 23;

            // Prev / next arrows
            if (hasPrev) drawSmallArrow(context, QuiverItem.getArrowStack(quiver, prevIdx), x - 14, y + 6);
            if (hasNext) drawSmallArrow(context, QuiverItem.getArrowStack(quiver, nextIdx), x + 25, y + 6);

            // Slot background — UV (24, 22) size (29, 24) in widgets.png
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            context.drawTexture(new net.minecraft.util.Identifier("textures/gui/widgets.png"), x, y, 24, 22, 29, 24);

            // Horizontal slide: scroll right → item enters from right (+X), scroll left → from left (-X)
            if (sel2 != lastArrowSel) {
                arrowXOffset = lastArrowSel >= 0 ? (lastArrowSel < sel2 ? 14f : -14f) : 0f;
                lastArrowSel = sel2;
            }
            arrowXOffset *= 0.78f;
            if (Math.abs(arrowXOffset) < 0.2f) arrowXOffset = 0f;

            int itemX = x + 3 + Math.round(arrowXOffset);
            context.enableScissor(x, y, x + 29, y + 24);
            context.drawItem(displayArrow, itemX, y + 4);
            context.disableScissor();

            // Text: shift right only when next is visible
            int textX = hasNext ? x + 44 : x + 30;
            // Include trail color name if present (Advanced Fletching Table compat)
            String baseName = displayArrow.getName().getString();
            net.minecraft.nbt.NbtCompound arrowTag = displayArrow.getNbt();
            String trailBracket = null;
            int trailColor = 0xFFFFFF;
            if (arrowTag != null && arrowTag.contains("TrailColorName")) {
                String raw = arrowTag.getString("TrailColorName");
                if (!raw.isEmpty()) {
                    // Capitalize first letter
                    String capitalized = Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
                    trailBracket = " [" + capitalized + "]";
                    if (arrowTag.contains("TrailColor"))
                        trailColor = arrowTag.getInt("TrailColor") | 0xFF000000;
                }
            }
            context.drawTextWithShadow(mc.textRenderer, baseName, textX, y + 2, 0xFFFFFF);
            if (trailBracket != null) {
                int nameW = mc.textRenderer.getWidth(baseName);
                context.drawTextWithShadow(mc.textRenderer, trailBracket, textX + nameW, y + 2, trailColor);
            }

            int count = displayArrow.getCount();
            String countText = String.valueOf(count);
            int countColor = arrowCountColor(count);
            context.drawTextWithShadow(mc.textRenderer, countText, textX, y + 12, countColor);
        });

        registerPredicates(ModItems.QUIVER);
        for (var item : new net.minecraft.item.Item[]{
                ModItems.COPPER_QUIVER, ModItems.IRON_QUIVER, ModItems.GOLD_QUIVER,
                ModItems.DIAMOND_QUIVER, ModItems.NETHERITE_QUIVER }) {
            registerPredicates(item);
            ColorProviderRegistry.ITEM.register((stack, tintIndex) -> dyeColor(stack, tintIndex), item);
        }

        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof QuiverTooltipData d) return new QuiverTooltipComponent(d);
            return null;
        });

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof HandledScreen<?>)) return;
            ScreenMouseEvents.allowMouseScroll(screen).register(
                (s, mouseX, mouseY, horiz, vert) -> handleQuiverScroll(s, mouseX, mouseY, vert)
            );
        });

        // Level-specific Conservation description in tooltips (works with or without EnchDesc)
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            int level = EnchantmentHelper.getLevel(Enchantments.INFINITY, stack);
            if (level <= 0) return;
            String key = level >= 2
                ? "enchantment.minecraft.infinity.desc.2"
                : "enchantment.minecraft.infinity.desc.1";
            Text desc = Text.translatable(key).formatted(Formatting.GRAY);
            // Remove any empty description EnchDesc may have added for Conservation
            lines.removeIf(line -> line != null && line.getString().isEmpty());
            // Insert right after the "Conservation" enchantment name line
            String enchName = Text.translatable("enchantment.minecraft.infinity").getString();
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i) != null && lines.get(i).getString().contains(enchName)) {
                    lines.add(i + 1, desc);
                    return;
                }
            }
            lines.add(desc); // fallback: append at end
        });

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> dyeColor(stack, tintIndex), ModItems.QUIVER);

        TrinketRendererRegistry.registerRenderer(ModItems.QUIVER,           new QuiverTrinketRenderer());
        TrinketRendererRegistry.registerRenderer(ModItems.COPPER_QUIVER,    new QuiverTrinketRenderer());
        TrinketRendererRegistry.registerRenderer(ModItems.IRON_QUIVER,      new QuiverTrinketRenderer());
        TrinketRendererRegistry.registerRenderer(ModItems.GOLD_QUIVER,      new QuiverTrinketRenderer());
        TrinketRendererRegistry.registerRenderer(ModItems.DIAMOND_QUIVER,   new QuiverTrinketRenderer());
        TrinketRendererRegistry.registerRenderer(ModItems.NETHERITE_QUIVER, new QuiverTrinketRenderer());
    }

    /** Renders an arrow item at 75% scale. */
    private static void drawSmallArrow(net.minecraft.client.gui.DrawContext context, net.minecraft.item.ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(0.75f, 0.75f, 1f);
        context.drawItem(stack, 0, 0);
        context.getMatrices().pop();
    }

    /** Progressive color: red (0) → yellow (32) → green (64+). Same scale as the quiver bar. */
    private static int arrowCountColor(int count) {
        if (count >= 64) return 0x55FF55;
        float t = Math.max(0, count) / 64.0f;
        int r, g;
        if (t <= 0.5f) {
            r = 0xFF;
            g = (int)(0x55 + (0xFF - 0x55) * (t / 0.5f));
        } else {
            r = (int)(0xFF - (0xFF - 0x55) * ((t - 0.5f) / 0.5f));
            g = 0xFF;
        }
        return 0xFF000000 | (r << 16) | (g << 8) | 0x55;
    }

    private static void registerPredicates(net.minecraft.item.Item item) {
        ModelPredicateProviderRegistry.register(item, new Identifier("quiver", "has_arrows"),
            (stack, world, entity, seed) -> QuiverItem.getTotalCount(stack) > 0 ? 1.0f : 0.0f);
        ModelPredicateProviderRegistry.register(item, new Identifier("quiver", "hip_mode"),
            (stack, world, entity, seed) -> QuiverTrinketRenderer.HIP_MODE.get() ? 1.0f : 0.0f);
        ModelPredicateProviderRegistry.register(item, new Identifier("quiver", "baldric_mode"),
            (stack, world, entity, seed) -> QuiverTrinketRenderer.BALDRIC_MODE.get() ? 1.0f : 0.0f);
    }

    private static int dyeColor(net.minecraft.item.ItemStack stack, int tintIndex) {
        if (tintIndex != 0) return -1;
        DyeableItem dyeable = (DyeableItem) stack.getItem();
        if (!dyeable.hasColor(stack)) return DyeableItem.DEFAULT_COLOR;
        int raw = dyeable.getColor(stack);
        int r = (raw >> 16) & 0xFF, g = (raw >> 8) & 0xFF, b = raw & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        if (max > 0) { r = r * 255 / max; g = g * 255 / max; b = b * 255 / max; }
        return (r << 16) | (g << 8) | b;
    }

    private static boolean handleQuiverScroll(Screen screen, double mouseX, double mouseY, double vert) {
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        HandledScreen<?> hs = (HandledScreen<?>) screen;
        int sx = accessor.getX(), sy = accessor.getY();
        // Check if player has an equipped quiver (for Trinkets slot detection)
        boolean hasEquippedQuiver = MinecraftClient.getInstance().player != null &&
            dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(MinecraftClient.getInstance().player)
                .map(comp -> comp.getAllEquipped().stream()
                    .anyMatch(pair -> pair.getRight().getItem() instanceof QuiverItem))
                .orElse(false);

        Slot quiverSlot = null;
        for (Slot slot : hs.getScreenHandler().slots) {
            double rx = mouseX - sx, ry = mouseY - sy;
            if (rx < slot.x || rx >= slot.x + 16 || ry < slot.y || ry >= slot.y + 16) continue;
            // Match by stack content OR by Trinkets slot type when quiver is equipped
            if (slot.getStack().getItem() instanceof QuiverItem) { quiverSlot = slot; break; }
            if (hasEquippedQuiver && slot instanceof dev.emi.trinkets.TrinketSlot) { quiverSlot = slot; break; }
        }
        if (quiverSlot == null) return true;
        int direction = vert < 0 ? 1 : -1;

        // Find which Trinkets slot the quiver is in (same logic as HandledScreenScrollMixin)
        String slotGroup = "";
        String slotName  = "";
        if (quiverSlot instanceof dev.emi.trinkets.TrinketSlot && MinecraftClient.getInstance().player != null) {
            var compOpt = dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(MinecraftClient.getInstance().player);
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
        return false;
    }
}
