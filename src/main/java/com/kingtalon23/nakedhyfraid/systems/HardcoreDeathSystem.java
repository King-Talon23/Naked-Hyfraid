package com.kingtalon23.nakedhyfraid.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kingtalon23.nakedhyfraid.NakedHyfraidPlugin;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

/**
 * One life: when a player dies they are recorded as dead and disconnected. They can't rejoin this save
 * until an admin runs {@code /hardcore revive}. Hytale only has Adventure and Creative game modes (no
 * spectator), so locking the player out is the hardcore penalty.
 */
public class HardcoreDeathSystem extends DeathSystems.OnDeathSystem {
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType());
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull DeathComponent component,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        NakedHyfraidPlugin plugin = NakedHyfraidPlugin.get();
        if (!plugin.getConfig().isHardcore()) {
            return;
        }
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        plugin.getDeadPlayers().markDead(playerRef.getUuid(), playerRef.getUsername());
        Universe.get().sendMessage(Message.raw(playerRef.getUsername() + " has died. Hardcore: no second chances."));

        long delayMs = (long) (plugin.getConfig().getDeathKickDelaySeconds() * 1000.0f);
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> playerRef.getPacketHandler().disconnect(NakedHyfraidPlugin.DEATH_MESSAGE),
                delayMs,
                TimeUnit.MILLISECONDS
        );
    }
}
