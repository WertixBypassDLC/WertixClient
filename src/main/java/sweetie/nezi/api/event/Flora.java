package sweetie.nezi.api.event;

import sweetie.nezi.api.event.interfaces.Cacheable;
import sweetie.nezi.api.event.interfaces.Notifiable;
import sweetie.nezi.api.event.interfaces.Subscribable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class Flora<T> implements Cacheable<T>, Subscribable<Listener<T>, T>, Notifiable<T> {
    private final List<Listener<T>> listeners = new CopyOnWriteArrayList<>();

    @SuppressWarnings("unchecked")
    private volatile Listener<T>[] cache = (Listener<T>[]) new Listener<?>[0];

    private volatile boolean rebuildCache = true;

    @Override
    @SuppressWarnings("unchecked")
    public Listener<T>[] getCache() {
        if (rebuildCache) {
            cache = listeners.toArray(new Listener[0]);
            rebuildCache = false;
        }
        return cache;
    }

    @Override
    public EventListener subscribe(Listener<T> listener) {
        listeners.add(listener);
        listeners.sort(null);
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
        for (Listener<T> tListener : getCache()) {
            try {
                tListener.getHandler().accept(event);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }
}