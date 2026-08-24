package dev.client.api.nullcry.uiClient.draggables;

import dev.client.Just;
import dev.client.api.nullcry.ClientApi;
import dev.client.api.nullcry.IDraggable;
import dev.client.api.nullcry.IModule;
import dev.client.api.nullcry.events.core.network.UpdateEvent;
import dev.client.api.nullcry.events.core.player.PlayerAttackEvent;
import dev.client.api.nullcry.events.core.render.RenderEvent;

public interface IHelper extends ClientApi, IModule, IDraggable {
    void onRender(RenderEvent.Draw2D event);
    default void onUpdate(UpdateEvent event) {}
    default Just handlerClient() {return IModule.super.handlerClient();}
    default void onPlayerAttack(PlayerAttackEvent event) {}
}