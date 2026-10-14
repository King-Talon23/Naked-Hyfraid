package com.kingtalon23.nakedhyfraid.hardcore;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;

import java.util.Collection;
import java.util.logging.Level;

/**
 * Stops food from healing.
 *
 * <p>In vanilla Hytale, food heals by applying entity effects such as {@code Food_Instant_Heal_T1} or
 * {@code Food_Health_Regen_Small}, whose {@code StatModifiers} add Health. Health potions use their own
 * effects, which don't start with {@code Food_}. So for every effect whose id matches a configured
 * prefix, we drop the positive Health entry and leave everything else (stamina, buffs, damage) alone.
 * The client receives the same map in the effect packet, so its prediction stays in sync.
 */
public final class FoodHealingRemover {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private FoodHealingRemover() {
    }

    public static void strip(Collection<EntityEffect> effects, String[] prefixes) {
        int health = DefaultEntityStatTypes.getHealth();
        for (EntityEffect effect : effects) {
            Int2FloatMap stats = effect.getEntityStats();
            if (stats == null || !stats.containsKey(health) || stats.get(health) <= 0.0f) {
                continue;
            }
            String id = effect.getId();
            if (id != null && matches(id, prefixes)) {
                stats.remove(health);
                LOGGER.at(Level.INFO).log("Removed healing from food effect %s", id);
            } else {
                // Logged so server owners can spot healing effects to add to FoodEffectPrefixes.
                LOGGER.at(Level.FINE).log("Left healing effect alone: %s", id);
            }
        }
    }

    private static boolean matches(String id, String[] prefixes) {
        for (String prefix : prefixes) {
            if (!prefix.isEmpty() && id.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
