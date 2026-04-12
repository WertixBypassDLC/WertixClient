package sweetie.nezi.client.features.modules.movement.spider.modes;

import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import sweetie.nezi.api.event.events.player.move.MotionEvent;
import sweetie.nezi.api.utils.math.TimerUtil;
import sweetie.nezi.client.features.modules.movement.spider.SpiderMode;
import sweetie.nezi.client.features.modules.movement.spider.SpiderModule;

public class SpiderSeaCucumber extends SpiderMode {

    @Override
    public String getName() {
        return "Морской огурец";
    }

    private final TimerUtil timerUtil = new TimerUtil();
    private Double startY = null;

    @Override
    public void onMotion(MotionEvent.MotionEventData event) {
        if (mc.player == null || mc.world == null) return;

        // Сбрасываем отсчёт когда игрок на земле и не у стены
        if (mc.player.isOnGround() && !hozColl()) {
            startY = null;
            return;
        }

        if (!hozColl()) return;

        // Запоминаем Y при первом касании стены
        if (startY == null) {
            startY = mc.player.getY();
        }

        // После 3 блоков — разгон и отключение
        if (mc.player.getY() - startY >= 3.0) {
            Vec3d vel = mc.player.getVelocity();
            mc.player.setVelocity(vel.x, 0.55, vel.z);
            mc.player.fallDistance = 0f;
            startY = null;
            SpiderModule.getInstance().toggle();
            return;
        }

        if (!timerUtil.finished(220)) return;

        event.ground(true);
        mc.player.setOnGround(true);
        mc.player.jump();

        int slot = findSeaPickle();
        if (slot == -1) return;

        placeAtFeet(slot);
        mc.player.fallDistance = 0f;
        timerUtil.reset();
    }

    private int findSeaPickle() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.SEA_PICKLE) {
                return i;
            }
        }
        return -1;
    }

    private void placeAtFeet(int slot) {
        int saved = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;
        mc.player.setPitch(75f);

        BlockHitResult hit = raycast(mc.player.getYaw(), 75f, 4.5);
        if (hit.getType() == HitResult.Type.BLOCK) {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        mc.player.getInventory().selectedSlot = saved;
    }

    private BlockHitResult raycast(float yaw, float pitch, double distance) {
        Vec3d eye = mc.player.getEyePos();
        Vec3d dir = Vec3d.fromPolar(pitch, yaw).normalize();
        Vec3d end = eye.add(dir.multiply(distance));
        return mc.world.raycast(new RaycastContext(
                eye, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player));
    }
}
