package com.kingtalon23.nakedhyfraid.hardcore;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapLoadException;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.DisabledWorldMapProvider;

import java.util.logging.Level;

/**
 * Turns the world map off using the server's own {@code Disabled} map provider. That provider reports
 * {@code WorldMapSettings.DISABLED}, whose settings packet tells clients the map is not enabled.
 */
public final class MapDisabler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private MapDisabler() {
    }

    /**
     * For a world that hasn't finished loading yet (AddWorldEvent fires before {@code World.init()} picks
     * the map generator from the world config).
     */
    public static void beforeInit(World world) {
        world.getWorldConfig().setWorldMapProvider(new DisabledWorldMapProvider());
    }

    /** For a world that is already running: swap its live map generator and push the new settings. */
    public static void whileRunning(World world) {
        beforeInit(world);
        world.execute(() -> {
            try {
                world.getWorldMapManager().setGenerator(new DisabledWorldMapProvider().getGenerator(world));
            } catch (WorldMapLoadException e) {
                LOGGER.at(Level.WARNING).withCause(e).log("Could not disable the map in world %s", world.getName());
            }
        });
    }
}
