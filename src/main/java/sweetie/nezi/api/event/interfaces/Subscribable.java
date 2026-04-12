package sweetie.nezi.api.event.interfaces;

import sweetie.nezi.api.event.EventListener;

public interface Subscribable<L, T> {
    EventListener subscribe(L listener);
    void unsubscribe(L listener);
}
