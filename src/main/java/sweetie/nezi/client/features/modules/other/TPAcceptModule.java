package sweetie.nezi.client.features.modules.other;

import lombok.Getter;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import sweetie.nezi.api.event.Listener;
import sweetie.nezi.api.event.EventListener;
import sweetie.nezi.api.event.events.client.PacketEvent;
import sweetie.nezi.api.module.Category;
import sweetie.nezi.api.module.Module;
import sweetie.nezi.api.module.ModuleRegister;
import sweetie.nezi.api.system.configs.FriendManager;

@ModuleRegister(name = "TP Accept", category = Category.OTHER)
public class TPAcceptModule extends Module {
    @Getter private static final TPAcceptModule instance = new TPAcceptModule();

    @Override
    public void onEvent() {
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.isReceive() && event.packet() instanceof GameMessageS2CPacket packet) {
                String message = packet.content().getString();

                if (message.contains("телепортироваться") || message.contains("tpaccept")) {
                    for (String name : FriendManager.getInstance().getData()) {
                        if (message.toLowerCase().contains(name.toLowerCase())) {
                            mc.player.networkHandler.sendChatCommand("tpaccept " + name);
                            break;
                        }
                    }
                }
            }
        }));

        addEvents(packetEvent);
    }
}
