package sweetie.nezi.client.features.modules.movement.spider;

import sweetie.nezi.api.event.events.player.move.MotionEvent;
import sweetie.nezi.api.system.backend.Choice;

public abstract class SpiderMode extends Choice {
    public void onUpdate() {}
    public void onMotion(MotionEvent.MotionEventData event) {}

    public boolean hozColl() {
        return mc.player.horizontalCollision;
    }
}
