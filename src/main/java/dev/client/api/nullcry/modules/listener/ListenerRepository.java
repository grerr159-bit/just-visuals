package dev.client.api.nullcry.modules.listener;

import dev.client.Just;
import dev.client.api.nullcry.modules.listener.impl.EventListener;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListenerRepository {
    final List<Listener> listeners = new ArrayList<>();
    
    public void setup() {
        registerListeners(new EventListener());
    }

    public void registerListeners(Listener... listeners) {
        this.listeners.addAll(List.of(listeners));
        Arrays.stream(listeners).forEach(listener -> Just.getInstance().getEventBus().register(listener));
    }
}
