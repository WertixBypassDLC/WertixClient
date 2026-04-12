package sweetie.nezi.api.event.events.other;

import lombok.Getter;
import sweetie.nezi.api.event.events.Event;

public class RotationUpdateEvent extends Event<RotationUpdateEvent> {
    @Getter private static final RotationUpdateEvent instance = new RotationUpdateEvent();
}
