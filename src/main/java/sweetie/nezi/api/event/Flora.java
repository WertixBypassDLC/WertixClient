package sweetie.nezi.api.event;

import sweetie.nezi.api.event.interfaces.Cacheable;
import sweetie.nezi.api.event.interfaces.Notifiable;
import sweetie.nezi.api.event.interfaces.Subscribable;
import sweetie.nezi.api.system.interfaces.QuickImports;

import java.util.concurrent.ConcurrentSkipListSet;

public abstract class Flora<T> implements Cacheable<T>, Subscribable<Listener<T>, T>, Notifiable<T> {
    private final ConcurrentSkipListSet<Listener<T>> listeners = new ConcurrentSkipListSet<>();

    @SuppressWarnings("unchecked")
    private volatile Listener<T>[] cache = (Listener<T>[]) new Listener<?>[0];

    private volatile boolean rebuildCache = true;

    @Override
    @SuppressWarnings("unchecked")
    public Listener<T>[] getCache() {
        if (rebuildCache) {
            cache = listeners.toArray(Listener[]::new);
            rebuildCache = false;
        }
        return cache;
    }

    @Override
    public EventListener subscribe(Listener<T> listener) {
        listeners.add(listener);
        rebuildCache = true;
        return new EventListener(() -> unsubscribe(listener));
    }

    @Override
    public void unsubscribe(Listener<T> listener) {
        if (listeners.remove(listener))
            rebuildCache = true;
    }

    @Override
    public void notify(T event) {
        QuickImports.bindMinecraftClient();
        for (Listener<T> tListener : getCache()) {
            try {
                tListener.getHandler().accept(event);
            } catch (Throwable throwable) {
                System.err.println("Event listener failed in " + getClass().getSimpleName() + ": " + throwable.getMessage());
                throwable.printStackTrace();
            }
        }
    }
}
