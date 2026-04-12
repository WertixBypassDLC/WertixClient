package sweetie.nezi.api.utils.rotation.manager;

import lombok.Getter;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import sweetie.nezi.api.module.setting.ModeSetting;
import sweetie.nezi.api.system.interfaces.QuickImports;

@Getter
public abstract class RotationMode implements QuickImports, ModeSetting.NamedChoice {
    private final String name;

    public RotationMode(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public Rotation process(Rotation currentRotation, Rotation targetRotation) {
        return process(currentRotation, targetRotation, null, null);
    }

    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d) {
        return process(currentRotation, targetRotation, vec3d, null);
    }

    public abstract Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity);
}
