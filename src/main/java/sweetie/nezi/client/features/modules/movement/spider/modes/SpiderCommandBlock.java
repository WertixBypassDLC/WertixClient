package sweetie.nezi.client.features.modules.movement.spider.modes;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import sweetie.nezi.api.event.events.player.move.MotionEvent;
import sweetie.nezi.api.utils.math.TimerUtil;
import sweetie.nezi.client.features.modules.movement.spider.SpiderMode;

public class SpiderCommandBlock extends SpiderMode {

    @Override
    public String getName() {
        return "CommandBlock";
    }

    private final TimerUtil timer = new TimerUtil();

    @Override
    public void onMotion(MotionEvent.MotionEventData event) {
        if (mc.player == null || mc.world == null) return;

        if (!hozColl()) return;

        if (timer.finished(350)) {
            // Как в Pandora: выставляем флаги земли и прыгаем
            event.ground(true);
            mc.player.setOnGround(true);
            // mc.player.collidedVertically = true; // Если есть в твоих маппингах
            mc.player.jump();
            timer.reset();

            int slot = getCommandBlockSlot(true);
            if (slot != -1 && mc.player.fallDistance > 0 && mc.player.fallDistance < 1.5f) {
                placeCommandBlock(event, slot);
            }
        }
    }

    private void placeCommandBlock(MotionEvent.MotionEventData event, int slot) {
        if (mc.interactionManager == null) return;

        ClientPlayerEntity p = mc.player;
        PlayerInventory inv = p.getInventory();

        int lastSlot = inv.selectedSlot;
        inv.selectedSlot = slot;

        // 1. Устанавливаем углы в MotionEvent (уйдет на сервер)
        float targetPitch = 80.0f;
        float targetYaw = (float) yawFromFacing(p.getHorizontalFacing());

        event.pitch(targetPitch);
        event.yaw(targetYaw);

        // 2. Делаем RayTrace по КАССТОМНЫМ углам (как MouseUtil в Pandora)
        BlockHitResult bhr = rayTrace(4.0, targetYaw, targetPitch);

        if (bhr != null && bhr.getType() == HitResult.Type.BLOCK) {
            p.swingHand(Hand.MAIN_HAND);
            mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, bhr);
            p.fallDistance = 0.0f;
        }

        inv.selectedSlot = lastSlot;
    }

    // Аналог MouseUtil.rayTrace из Pandora
    private BlockHitResult rayTrace(double distance, float yaw, float pitch) {
        Vec3d start = mc.player.getCameraPosVec(1.0f);

        // Математика поворота вектора
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = (float) Math.cos(g);
        float i = (float) Math.sin(g);
        float j = (float) Math.cos(f);
        float k = (float) Math.sin(f);
        Vec3d dir = new Vec3d(i * j, -k, h * j);

        Vec3d end = start.add(dir.multiply(distance));

        return mc.world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player));
    }

    private int getCommandBlockSlot(boolean inHotBar) {
        int first = inHotBar ? 0 : 9;
        int last = inHotBar ? 9 : 36;
        for (int i = first; i < last; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.REPEATING_COMMAND_BLOCK) {
                return i;
            }
        }
        return -1;
    }

    private static double yawFromFacing(Direction d) {
        if (d == Direction.SOUTH) return 0.0;
        if (d == Direction.WEST)  return 90.0;
        if (d == Direction.NORTH) return 180.0;
        if (d == Direction.EAST)  return -90.0;
        return 0.0;
    }
}