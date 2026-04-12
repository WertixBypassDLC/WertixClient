package sweetie.nezi.api.event.events.player.other;

import lombok.Getter;
import sweetie.nezi.api.event.events.Event;

public class PostRotationMovementInputEvent extends Event<PostRotationMovementInputEvent> {
    @Getter private static final PostRotationMovementInputEvent instance = new PostRotationMovementInputEvent();
}
