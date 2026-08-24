package dev.client.api.nullcry.events;

import dev.client.Just;

public class EventManager {

    public static void register(Object listener) {
        Just.getInstance().getEventBus().register(listener);
    }

    public static void unregister(Object listener) {
        Just.getInstance().getEventBus().unregister(listener);
    }

    public static void call(Event event) {
        Just.getInstance().getEventBus().post(event);
    }

    public static void cancelled(Event event) {
        event.cancelled();
    }

    public static boolean isCancelled(Event event) {
        return event.isCancelled();
    }

}
