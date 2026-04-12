package sweetie.nezi.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.Entity;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.system.configs.FriendManager;

@ModuleRegister(name = "No Entity Trace", category = Category.COMBAT)
public class NoEntityTraceModule extends Module {
    @Getter private static final NoEntityTraceModule instance = new NoEntityTraceModule();

    @Override
    public void onEvent() {

    }

    public boolean shouldCancelResult(Entity entity) {
        boolean noFriendHurt = NoFriendHurtModule.getInstance().isEnabled() &&
                entity != null && FriendManager.getInstance().contains(entity.getName().getString());
        return noFriendHurt || isEnabled();
    }
}
