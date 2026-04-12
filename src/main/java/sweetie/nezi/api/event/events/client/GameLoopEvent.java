package sweetie.nezi.api.event.events.client;

import lombok.Getter;
import sweetie.nezi.api.event.events.Event;

public class GameLoopEvent extends Event<GameLoopEvent> {
    @Getter private static final GameLoopEvent instance = new GameLoopEvent();
}
