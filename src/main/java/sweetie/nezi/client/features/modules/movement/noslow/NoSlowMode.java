package sweetie.nezi.client.features.modules.movement.noslow;

import sweetie.nezi.api.event.events.player.move.MoveEvent;
import sweetie.nezi.api.system.backend.Choice;

public abstract class NoSlowMode extends Choice {
    public void onEnable() {}
    public void onDisable() {}
    public abstract void onUpdate();
    public abstract void onTick();
    public abstract boolean slowingCancel();
    public void onMove(MoveEvent.MoveEventData data) {}
}
