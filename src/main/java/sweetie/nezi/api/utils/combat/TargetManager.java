package sweetie.nezi.api.utils.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;
import java.util.regex.Pattern;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Getter;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.system.configs.FriendManager;
import sweetie.nezi.api.system.interfaces.QuickImports;
import sweetie.nezi.api.utils.rotation.RotationUtil;

public class TargetManager implements QuickImports {
    private static final Pattern STATIONARY_NUMERIC_NAME = Pattern.compile("\\d{3,5}");
    @Getter private LivingEntity currentTarget;
    private Stream<LivingEntity> potentialTargets;

    public void lockTarget(LivingEntity target) {
        if (currentTarget == null) {
            currentTarget = target;
        }
    }

    public void releaseTarget() {
        currentTarget = null;
    }

    public void validateTarget(Predicate<LivingEntity> predicate) {
        findFirstMatch(predicate).ifPresent(this::lockTarget);

        if (currentTarget != null && !predicate.test(currentTarget)) {
            releaseTarget();
        }
    }

    public void searchTargets(Iterable<Entity> entities, float maxDistance) {
        if (isTargetOutOfRange(maxDistance)) {
            releaseTarget();
        }

        potentialTargets = createStreamFromEntities(entities, maxDistance);
    }

    private boolean isTargetOutOfRange(float maxDistance) {
        return currentTarget != null && RotationUtil.getSpot(currentTarget).distanceTo(mc.player.getEyePos()) > maxDistance;
    }

    private Stream<LivingEntity> createStreamFromEntities(Iterable<Entity> entities, float maxDistance) {
        return StreamSupport.stream(entities.spliterator(), false)
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .filter(it -> {
                    Vec3d spot = RotationUtil.getSpot(it);
                    return mc.player.getEyePos().distanceTo(spot) <= maxDistance;
                })
                .sorted(java.util.Comparator.comparingDouble(it -> {
                    Vec3d spot = RotationUtil.getSpot(it);
                    return mc.player.getEyePos().distanceTo(spot);
                }));
    }

    private java.util.Optional<LivingEntity> findFirstMatch(java.util.function.Predicate<LivingEntity> predicate) {
        return potentialTargets != null ? potentialTargets.filter(predicate).findFirst() : java.util.Optional.empty();
    }

    public static boolean isStationaryNumericNamePlayer(PlayerEntity player) {
        if (player == null) {
            return false;
        }

        String name = player.getGameProfile().getName();
        if (name == null || !STATIONARY_NUMERIC_NAME.matcher(name).matches()) {
            return false;
        }

        return !isActuallyMoving(player);
    }

    public static boolean isActuallyMoving(PlayerEntity player) {
        if (player == null) {
            return false;
        }

        double dx = player.getX() - player.prevX;
        double dz = player.getZ() - player.prevZ;
        double horizontalDeltaSq = dx * dx + dz * dz;
        return horizontalDeltaSq > 1.0E-4D || player.getVelocity().horizontalLengthSquared() > 1.0E-4D;
    }

    public static class EntityFilter implements QuickImports {
        public List<String> targetSettings;
        public boolean needFriends = false;

        public EntityFilter(List<String> targetSettings) {
            this.targetSettings = targetSettings;
        }

        public boolean isValid(LivingEntity entity) {
            if (isLocalPlayer(entity)) return false;
            if (isInvalidHealth(entity)) return false;
            if (isBotPlayer(entity)) return false;

            return isValidEntityType(entity);
        }

        private boolean isLocalPlayer(LivingEntity entity) {
            return entity == mc.player;
        }

        private boolean isInvalidHealth(LivingEntity entity) {
            return !entity.isAlive() || entity.getHealth() <= 0;
        }

        private boolean isBotPlayer(LivingEntity entity) {
            return entity == mc.player.getControllingVehicle();
        }

        private boolean isValidEntityType(LivingEntity entity) {
            if (entity instanceof PlayerEntity player) {
                if (TargetManager.isStationaryNumericNamePlayer(player)) {
                    return false;
                }
                if (FriendManager.getInstance().contains(player.getName().getString()) && !needFriends) {
                    return false;
                }
                
                boolean isNaked = true;
                for (net.minecraft.item.ItemStack armor : player.getArmorItems()) {
                    if (!armor.isEmpty()) { isNaked = false; break; }
                }
                if (isNaked) {
                    return targetSettings.contains("Игрок голый") || targetSettings.contains("Голые") || targetSettings.contains("Naked");
                } else {
                    return targetSettings.contains("Игрок") || targetSettings.contains("Игроки") || targetSettings.contains("Players");
                }
            } else if (entity instanceof net.minecraft.entity.passive.VillagerEntity) {
                return targetSettings.contains("Жители") || targetSettings.contains("Villagers");
            } else if (entity instanceof AnimalEntity) {
                return targetSettings.contains("Животные") || targetSettings.contains("Animals");
            } else if (entity instanceof MobEntity) {
                return targetSettings.contains("Мобы") || targetSettings.contains("Mobs");
            } else return false;
        }
    }
}
