package sweetie.nezi.client.features.modules.movement.fly;

import sweetie.nezi.api.event.events.player.move.MotionEvent;
import sweetie.nezi.api.system.backend.Choice;

public abstract class FlightMode extends Choice {


    // events
    public void onUpdate() {}
    public void onMotion(MotionEvent.MotionEventData event) {}

    // module methods
    public void onEnable() {}
    public void onDisable() {}
    public void toggle() {}
}
