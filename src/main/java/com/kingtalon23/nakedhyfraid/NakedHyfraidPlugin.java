package com.kingtalon23.nakedhyfraid;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.server.core.Constants;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.hypixel.hytale.server.core.util.Config;
import com.kingtalon23.nakedhyfraid.commands.HardcoreCommand;
import com.kingtalon23.nakedhyfraid.hardcore.DeadPlayerStore;
import com.kingtalon23.nakedhyfraid.hardcore.FoodHealingRemover;
import com.kingtalon23.nakedhyfraid.hardcore.MapDisabler;
import com.kingtalon23.nakedhyfraid.systems.ArmourPenaltySystem;
import com.kingtalon23.nakedhyfraid.systems.HardcoreDeathSystem;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Naked & Hyfraid: one-life hardcore, no world map, armour hurts, food doesn't heal (potions still do).
 */
public class NakedHyfraidPlugin extends JavaPlugin {
    public static final Message DEATH_MESSAGE = Message.raw("You died. This world is hardcore, so that was your only life.");

    private static NakedHyfraidPlugin instance;

    private final Config<NakedHyfraidConfig> config;
    private DeadPlayerStore deadPlayers;
    private ArmourPenaltySystem armourPenaltySystem;

    public NakedHyfraidPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        this.config = this.withConfig(NakedHyfraidConfig.CODEC);
    }

    public static NakedHyfraidPlugin get() {
        return instance;
    }

    public NakedHyfraidConfig getConfig() {
        return config.get();
    }

    public DeadPlayerStore getDeadPlayers() {
        return deadPlayers;
    }

    @Override
    protected void setup() {
        super.setup();
        config.save();

        // Per save, so each singleplayer world (universe) tracks its own deaths.
        deadPlayers = new DeadPlayerStore(Constants.UNIVERSE_PATH.resolve("naked-hyfraid").resolve("dead-players.txt"));
        deadPlayers.load();

        armourPenaltySystem = new ArmourPenaltySystem();
        getEntityStoreRegistry().registerSystem(new HardcoreDeathSystem());
        getEntityStoreRegistry().registerSystem(armourPenaltySystem);

        // Dead players can't come back.
        getEventRegistry().register(PlayerSetupConnectEvent.class, event -> {
            if (getConfig().isHardcore() && deadPlayers.isDead(event.getUuid())) {
                event.setReason(DEATH_MESSAGE);
                event.setCancelled(true);
            }
        });
        getEventRegistry().register(PlayerDisconnectEvent.class,
                event -> armourPenaltySystem.forget(event.getPlayerRef().getUuid()));

        // No map in any world, including ones created later.
        getEventRegistry().registerGlobal(AddWorldEvent.class, event -> {
            if (getConfig().isDisableWorldMap()) {
                MapDisabler.beforeInit(event.getWorld());
            }
        });

        // Food stops healing; re-scan whenever effects, items or interactions (re)load.
        getEventRegistry().register(LoadedAssetsEvent.class, EntityEffect.class, this::onEffectsLoaded);
        getEventRegistry().register(LoadedAssetsEvent.class, Item.class, this::onItemsLoaded);
        getEventRegistry().register(LoadedAssetsEvent.class, RootInteraction.class, this::onInteractionsLoaded);

        getCommandRegistry().registerCommand(new HardcoreCommand());
    }

    @Override
    protected void start() {
        super.start();
        if (getConfig().isDisableWorldMap()) {
            // Worlds that loaded before this plugin did.
            for (World world : Universe.get().getWorlds().values()) {
                MapDisabler.whileRunning(world);
            }
        }
        removeFoodHealing();
    }

    private void onEffectsLoaded(LoadedAssetsEvent<String, EntityEffect, IndexedLookupTableAssetMap<String, EntityEffect>> event) {
        removeFoodHealing();
    }

    private void onItemsLoaded(LoadedAssetsEvent<String, Item, DefaultAssetMap<String, Item>> event) {
        removeFoodHealing();
    }

    private void onInteractionsLoaded(LoadedAssetsEvent<String, RootInteraction, IndexedLookupTableAssetMap<String, RootInteraction>> event) {
        removeFoodHealing();
    }

    private void removeFoodHealing() {
        if (!getConfig().isDisableFoodHealing()) {
            return;
        }
        try {
            FoodHealingRemover.apply(getConfig().getFoodEffectPrefixes());
        } catch (RuntimeException e) {
            // An asset store that hasn't loaded yet during startup; start() scans again once everything is up.
            getLogger().at(Level.FINE).withCause(e).log("Food healing scan skipped");
        }
    }
}
