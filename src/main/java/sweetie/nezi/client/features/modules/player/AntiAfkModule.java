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

    private final SliderSetting minDelay = new SliderSetting("Min delay").value(10f).range(5f, 60f).step(1f);
    private final SliderSetting maxDelay = new SliderSetting("Max delay").value(20f).range(6f, 90f).step(1f);
    private final SliderSetting yawRange = new SliderSetting("Yaw range").value(10f).range(3f, 25f).step(1f);
    private final SliderSetting pitchRange = new SliderSetting("Pitch range").value(4f).range(1f, 12f).step(1f);
    private final BooleanSetting randomJump = new BooleanSetting("Random jump").value(true);

    private long nextActionAt = 0L;
    private long jumpReleaseAt = 0L;

    public AntiAfkModule() {
        addSettings(minDelay, maxDelay, yawRange, pitchRange, randomJump);
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
        float yawOffset = (random.nextFloat() * 2f - 1f) * yawRange.getValue();
        float pitchOffset = (random.nextFloat() * 2f - 1f) * pitchRange.getValue();
        float newYaw = mc.player.getYaw() + yawOffset;
        float newPitch = MathHelper.clamp(mc.player.getPitch() + pitchOffset, -85f, 85f);

        mc.player.setYaw(newYaw);
        mc.player.setPitch(newPitch);
        mc.player.setHeadYaw(newYaw);
        mc.player.setBodyYaw(newYaw);

        if (randomJump.getValue() && mc.player.isOnGround() && random.nextFloat() < 0.55f) {
            tapJumpKey();
        }
    }

    private void scheduleNextAction() {
        float min = Math.min(minDelay.getValue(), maxDelay.getValue());
        float max = Math.max(minDelay.getValue(), maxDelay.getValue());
        long delayMs = (long) (ThreadLocalRandom.current().nextDouble(min, max + 0.001) * 1000.0);
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
