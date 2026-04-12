package sweetie.nezi.api.event.interfaces;

import sweetie.nezi.api.event.Listener;

public interface Cacheable<T> {
    Listener<T>[] getCache();
}