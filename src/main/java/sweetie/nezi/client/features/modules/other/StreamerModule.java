package sweetie.nezi.client.features.modules.other;

import lombok.Getter;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.module.setting.BooleanSetting;
import sweetie.nezi.api.system.configs.FriendManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ModuleRegister(name = "NameProtect", category = Category.OTHER)
public class StreamerModule extends Module {
    @Getter private static final StreamerModule instance = new StreamerModule();

    @Getter private final BooleanSetting hideNick = new BooleanSetting("Ник").value(true);
    @Getter private final BooleanSetting hideFriends = new BooleanSetting("Друзья").value(true).setVisible(hideNick::getValue);

    private final ConcurrentHashMap<String, Integer> friendCounter = new ConcurrentHashMap<>();
    private final AtomicInteger globalCounter = new AtomicInteger(1);

    public StreamerModule() {
        addSettings(hideNick, hideFriends);
    }

    public String getProtectedName() {
        return isEnabled() && hideNick.getValue() ? "Игрок" : mc.getSession().getUsername();
    }

    public String getProtectedFriendName(String name) {
        return isEnabled() && hideNick.getValue() && hideFriends.getValue() && FriendManager.getInstance().contains(name)
                ? generateProtectedFriendName(name)
                : name;
    }

    public String generateProtectedFriendName(String originalName) {
        int id = friendCounter.computeIfAbsent(originalName.toLowerCase(), key -> globalCounter.getAndIncrement());
        return "Друг " + id;
    }

    @Override
    public void onEvent() {
    }
}
