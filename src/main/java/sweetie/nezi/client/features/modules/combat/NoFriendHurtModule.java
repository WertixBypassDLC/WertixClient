package sweetie.nezi.client.features.modules.combat;

import lombok.Getter;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;

@ModuleRegister(name = "No Friend Hurt", category = Category.COMBAT)
public class NoFriendHurtModule extends Module {
    @Getter private static final NoFriendHurtModule instance = new NoFriendHurtModule();

    public NoFriendHurtModule() {
        setEnabled(true);
    }

    @Override
    public void onEvent() {

    }
}
