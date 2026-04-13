package sweetie.nezi.client.features.modules.player;

import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.events.player.other.UpdateEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.module.setting.SliderSetting;

import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "Anti AFK", category = Category.PLAYER)
public class AntiAfkModule extends Module {
    @Getter private static final AntiAfkModule instance = new AntiAfkModule();
    private static final long SPACE_TAP_MS = 110L;
    private long nextActionAt = 0L;
    private long jumpReleaseAt = 0L;

    public AntiAfkModule() {
    }

    @Override
    public void onEnable() {
        scheduleNextAction();
        jumpReleaseAt = 0L;
    }

    @Override
    public void onDisable() {
        nextActionAt = 0L;
        releaseJumpKey();
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> handleUpdate()));
        addEvents(update);
    }

    private void handleUpdate() {
        if (jumpReleaseAt > 0L && System.currentTimeMillis() >= jumpReleaseAt) {
            releaseJumpKey();
        }

        if (mc.player == null || mc.world == null) {
            return;
        }

        if (mc.currentScreen != null || mc.player.isUsingItem()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (nextActionAt == 0L) {
            scheduleNextAction();
            return;
        }

        if (now < nextActionAt) {
            return;
        }

        applyAntiAfkMotion();
        scheduleNextAction();
    }

    private void applyAntiAfkMotion() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float yawOffset = (random.nextFloat() * 2f - 1f) * 12f;
        float pitchOffset = (random.nextFloat() * 2f - 1f) * 6f;
        float newYaw = mc.player.getYaw() + yawOffset;
        float newPitch = MathHelper.clamp(mc.player.getPitch() + pitchOffset, -85f, 85f);

        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);
        mc.player.setHeadYaw(newYaw);
        mc.player.setBodyYaw(newYaw);

        if (mc.player.isOnGround() && random.nextFloat() < 0.6f) {
            tapJumpKey();
        }
    }

    private void scheduleNextAction() {
        long delayMs = (long) (ThreadLocalRandom.current().nextDouble(15.0, 25.0) * 1000.0);
        nextActionAt = System.currentTimeMillis() + delayMs;
    }

    private void tapJumpKey() {
        if (mc.options == null) {
            return;
        }

        mc.options.jumpKey.setPressed(true);
        jumpReleaseAt = System.currentTimeMillis() + SPACE_TAP_MS;
    }

    private void releaseJumpKey() {
        if (jumpReleaseAt == 0L || mc.options == null) {
            jumpReleaseAt = 0L;
            return;
        }

        mc.options.jumpKey.setPressed(false);
        jumpReleaseAt = 0L;
    }
}
