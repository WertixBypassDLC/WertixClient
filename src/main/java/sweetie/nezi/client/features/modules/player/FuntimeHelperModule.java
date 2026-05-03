package sweetie.nezi.client.features.modules.player;

import lombok.Getter;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.client.PacketEvent;
import sweetie.nezi.api.event.events.client.TickEvent;
import sweetie.nezi.api.event.events.render.Render2DEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BindSetting;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.system.backend.Pair;
import sweetie.nezi.api.utils.color.UIColors;
import sweetie.nezi.api.utils.math.MathUtil;
import sweetie.nezi.api.utils.math.ProjectionUtil;
import sweetie.nezi.api.utils.player.InventoryUtil;
import sweetie.nezi.api.utils.render.RenderUtil;
import sweetie.nezi.api.utils.render.fonts.Fonts;
import sweetie.nezi.client.features.modules.movement.InventoryMoveModule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ModuleRegister(name = "Funtime Helper", category = Category.PLAYER)
public class FuntimeHelperModule extends Module {
    @Getter private static final FuntimeHelperModule instance = new FuntimeHelperModule();

    private final BooleanSetting timer = new BooleanSetting("Таймер").value(true);
    private final Map<InventoryUtil.ItemUsage, BindSetting> keyBindings = new LinkedHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final List<Pair<Long, Vec3d>> consumables = new ArrayList<>();
    private final Map<Vec3d, String> consumableNames = new HashMap<>();

    public FuntimeHelperModule() {
        keyBindings.put(new InventoryUtil.ItemUsage(Items.PHANTOM_MEMBRANE, this), new BindSetting("Божья аура").value(-999));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.ENDER_EYE, this), new BindSetting("Дезорентация").value(-999));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.SUGAR, this), new BindSetting("Явная пыль").value(-999));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.FIRE_CHARGE, this), new BindSetting("Огненный смерч").value(-999));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.NETHERITE_SCRAP, this), new BindSetting("Трапка").value(-999));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.DRIED_KELP, this), new BindSetting("Пласт").value(-999));
        keyBindings.put(new InventoryUtil.ItemUsage(Items.SNOWBALL, this), new BindSetting("Snowball").value(-999));

        addSettings(timer);
        keyBindings.values().forEach(this::addSettings);
    }

    @Override
    public void onDisable() {
        keyBindings.keySet().forEach(InventoryUtil.ItemUsage::onDisable);
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> handleTickEvent()));
        EventListener renderEvent = Render2DEvent.getInstance().subscribe(new Listener<>(this::handleRenderEvent));
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(this::handlePacketEvent));
        addEvents(tickEvent, renderEvent, packetEvent);
    }

    private void handleRenderEvent(Render2DEvent.Render2DEventData event) {
        if (!timer.getValue()) return;

        MatrixStack matrixStack = event.matrixStack();
        consumables.removeIf(consumable -> (consumable.left() - System.currentTimeMillis()) <= 0L);

        for (Pair<Long, Vec3d> consumable : consumables) {
            Vec3d position = consumable.right();
            Vector2f screenPos = ProjectionUtil.project(position);
            if (screenPos.x == Float.MAX_VALUE || screenPos.y == Float.MAX_VALUE) {
                continue;
            }

            double time = MathUtil.round((double) (consumable.left() - System.currentTimeMillis()) / 1000.0D, 1);
            String name = consumableNames.getOrDefault(position, "Таймер");
            String text = name + ": " + time + "s";
            float size = 7f;
            float gap = 3f;
            float textWidth = Fonts.PS_BOLD.getWidth(text, size);
            float posX = screenPos.x - textWidth / 2f;
            float posY = screenPos.y;

            RenderUtil.BLUR_RECT.draw(matrixStack, posX, posY, textWidth + gap * 2f, size + gap * 2f, 2f, UIColors.blur());
            Fonts.PS_BOLD.drawText(matrixStack, text, posX + gap, posY + gap, size, UIColors.textColor());
        }
    }

    private void handlePacketEvent(PacketEvent.PacketEventData event) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client == null ? null : client.player;
        ClientWorld world = client == null ? null : client.world;
        if (!timer.getValue() || event.isSend() || player == null || world == null) {
            return;
        }

        if (!(event.packet() instanceof PlaySoundS2CPacket soundPacket)) {
            return;
        }

        String soundPath = soundPacket.getSound().getIdAsString();
        if (soundPath.equals("minecraft:block.piston.contract")) {
            Vec3d pos = Vec3d.ofCenter(new BlockPos((int) soundPacket.getX(), (int) soundPacket.getY(), (int) soundPacket.getZ()));
            consumables.add(new Pair<>(System.currentTimeMillis() + 15_000L, pos));
            consumableNames.put(pos, "Трапка");
            return;
        }

        if (!soundPath.equals("minecraft:block.anvil.place")) {
            return;
        }

        BlockPos soundPos = new BlockPos((int) soundPacket.getX(), (int) soundPacket.getY(), (int) soundPacket.getZ());
        long delay = 250L;
        float playerPitch = player.getPitch();

        scheduler.schedule(() -> getCube(soundPos, 4, 4).stream()
                .filter(pos -> getDistance(soundPos, pos) > 2 && world.getBlockState(pos).getBlock() == Blocks.COBBLESTONE)
                .min(Comparator.comparing(pos -> getDistance(soundPos, pos)))
                .ifPresent(pos -> {
                    if (getCube(pos, 1, 1).stream().anyMatch(p -> world.getBlockState(p).getBlock() == Blocks.ANVIL)) {
                        return;
                    }

                    long solidCount = getCube(pos, 1, 1).stream().filter(p -> {
                        BlockState state = world.getBlockState(p);
                        return !state.isAir() && state.isSolidBlock(world, p);
                    }).count();

                    if (solidCount >= 14) {
                        Vec3d addPos = Vec3d.ofCenter(pos);
                        long duration = Math.abs(playerPitch) >= 45.0f ? 60_000L - delay : 20_000L - delay;
                        consumables.add(new Pair<>(System.currentTimeMillis() + duration, addPos));
                        consumableNames.put(addPos, "Пласт");
                    } else if (solidCount >= 5) {
                        Vec3d addPos = Vec3d.ofCenter(pos).add(0, -1.5, 0);
                        consumables.add(new Pair<>(System.currentTimeMillis() + 15_000L - delay, addPos));
                        consumableNames.put(addPos, "Трапка");
                    }
                }), delay, TimeUnit.MILLISECONDS);
    }

    private void handleTickEvent() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        if (client.currentScreen != null && !InventoryMoveModule.getInstance().isEnabled()) return;
        boolean useLegitBypass = InventoryMoveModule.getInstance().usesLegitItemBypass();
        keyBindings.forEach((usage, bind) -> usage.handleUse(bind.getValue(), useLegitBypass));
    }

    private double getDistance(BlockPos pos1, BlockPos pos2) {
        double dx = pos1.getX() - pos2.getX();
        double dy = pos1.getY() - pos2.getY();
        double dz = pos1.getZ() - pos2.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private List<BlockPos> getCube(BlockPos center, int xRadius, int yRadius) {
        List<BlockPos> positions = new ArrayList<>();
        for (int x = -xRadius; x <= xRadius; x++) {
            for (int y = -yRadius; y <= yRadius; y++) {
                for (int z = -xRadius; z <= xRadius; z++) {
                    positions.add(center.add(x, y, z));
                }
            }
        }
        return positions;
    }
}
