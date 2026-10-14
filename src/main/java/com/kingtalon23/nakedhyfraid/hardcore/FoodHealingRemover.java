package com.kingtalon23.nakedhyfraid.hardcore;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.none.simple.ApplyEffectInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.operation.Operation;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Stops food from healing while leaving health potions alone.
 *
 * <p>Food and potions both heal by applying entity effects whose {@code StatModifiers} add Health. An
 * effect is treated as food healing when either:
 * <ul>
 *   <li>a food item applies it: we walk every food item's interactions and interaction vars (for example
 *       its {@code Effect} var, or shared ones like {@code HealthRegen_TierCheck_T2}) and collect every
 *       {@code ApplyEffect}, including effects defined inline in the item; or</li>
 *   <li>its id matches a configured food prefix ({@code Food_} by default), as a fallback for food
 *       added in ways the walk doesn't see.</li>
 * </ul>
 * Any effect a {@code Potion_} item applies is never touched, so potions always keep their healing.
 * For the matching effects only the positive Health entry is removed; stamina, buffs and damage stay.
 * The client gets the same map in the effect packet, so its prediction stays in sync.
 */
public final class FoodHealingRemover {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String POTION_PREFIX = "Potion_";
    private static final String FOOD_CONSUME_MARKER = "Consume_Food";

    private FoodHealingRemover() {
    }

    /** Re-scans all loaded items and effects. Safe to call repeatedly (e.g. after every asset reload). */
    public static void apply(String[] foodPrefixes) {
        int health = DefaultEntityStatTypes.getHealth();
        Map<String, EntityEffect> food = new LinkedHashMap<>();
        Map<String, EntityEffect> potion = new LinkedHashMap<>();

        for (Item item : Item.getAssetMap().getAssetMap().values()) {
            String id = normalize(item.getId());
            if (id.startsWith(POTION_PREFIX)) {
                collectEffects(item, potion);
            } else if (matches(id, foodPrefixes) || isConsumedAsFood(item)) {
                collectEffects(item, food);
            }
        }
        for (EntityEffect effect : EntityEffect.getAssetMap().getAssetMap().values()) {
            if (effect.getId() != null && matches(normalize(effect.getId()), foodPrefixes)) {
                food.putIfAbsent(effect.getId(), effect);
            }
        }

        for (Map.Entry<String, EntityEffect> entry : food.entrySet()) {
            Int2FloatMap stats = entry.getValue().getEntityStats();
            if (stats == null || !stats.containsKey(health) || stats.get(health) <= 0.0f) {
                continue;
            }
            if (potion.containsKey(entry.getKey())) {
                LOGGER.at(Level.WARNING).log("Effect %s is used by both food and potions; leaving its healing so potions still work", entry.getKey());
                continue;
            }
            stats.remove(health);
            LOGGER.at(Level.INFO).log("Removed healing from food effect %s", entry.getKey());
        }
    }

    private static boolean isConsumedAsFood(Item item) {
        Map<?, String> interactions = item.getInteractions();
        if (interactions == null) {
            return false;
        }
        for (String rootId : interactions.values()) {
            if (rootId != null && rootId.contains(FOOD_CONSUME_MARKER)) {
                return true;
            }
        }
        return false;
    }

    private static void collectEffects(Item item, Map<String, EntityEffect> out) {
        if (item.getInteractions() != null) {
            collectEffects(item.getInteractions().values(), out);
        }
        if (item.getInteractionVars() != null) {
            collectEffects(item.getInteractionVars().values(), out);
        }
    }

    private static void collectEffects(Collection<String> rootIds, Map<String, EntityEffect> out) {
        for (String rootId : rootIds) {
            if (rootId == null) {
                continue;
            }
            RootInteraction root = RootInteraction.getAssetMap().getAsset(rootId);
            if (root == null) {
                continue;
            }
            try {
                for (int i = 0; i < root.getOperationMax(); i++) {
                    Operation op = root.getOperation(i);
                    if (op != null && op.getInnerOperation() instanceof ApplyEffectInteraction apply) {
                        // The effect id field is private; the network packet carries its asset index.
                        int index = ((com.hypixel.hytale.protocol.ApplyEffectInteraction) apply.toPacket()).effectId;
                        EntityEffect effect = EntityEffect.getAssetMap().getAsset(index);
                        if (effect != null && effect.getId() != null) {
                            out.putIfAbsent(effect.getId(), effect);
                        }
                    }
                }
            } catch (RuntimeException e) {
                // Root not built yet (operations null) or an unexpected interaction shape: skip it.
                LOGGER.at(Level.FINE).withCause(e).log("Could not scan interaction %s", rootId);
            }
        }
    }

    /** Inline assets get ids like {@code *Food_Bread_Effect_0}; strip the leading marker. */
    private static String normalize(String id) {
        if (id == null) {
            return "";
        }
        int start = 0;
        while (start < id.length() && id.charAt(start) == '*') {
            start++;
        }
        return id.substring(start);
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
