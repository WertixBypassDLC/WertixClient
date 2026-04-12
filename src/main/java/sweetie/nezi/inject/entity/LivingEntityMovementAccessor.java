package sweetie.nezi.inject.entity;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityMovementAccessor {

    @Accessor("movementSpeed")
    float nezi$getMovementSpeed();

    @Accessor("movementSpeed")
    void nezi$setMovementSpeed(float value);
}
