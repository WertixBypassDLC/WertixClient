package sweetie.nezi.api.event.events.other;

import lombok.Getter;
import sweetie.nezi.api.event.events.Event;

public class WindowResizeEvent extends Event<WindowResizeEvent> {
    @Getter private static final WindowResizeEvent instance = new WindowResizeEvent();
}
