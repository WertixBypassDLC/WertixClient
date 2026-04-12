package sweetie.nezi.api.event.events.player.move;

import lombok.Getter;
import sweetie.nezi.api.event.events.Event;

public class TravelEvent extends Event<TravelEvent> {
    @Getter private static final TravelEvent instance = new TravelEvent();
}
