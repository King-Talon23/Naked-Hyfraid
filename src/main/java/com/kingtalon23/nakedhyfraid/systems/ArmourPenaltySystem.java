package com.kingtalon23.nakedhyfraid.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.kingtalon23.nakedhyfraid.NakedHyfraidConfig;
import com.kingtalon23.nakedhyfraid.NakedHyfraidPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hurts players for every armour piece they wear: each interval, deals
 * {@code ArmourDamagePerPiece x pieces worn} damage. Wear nothing and you take nothing.
 */
public class ArmourPenaltySystem extends EntityTickingSystem<EntityStore> {
    /** Id of the damage cause shipped in {@code Server/Entity/Damage/NakedHyfraid_Armour.json}. */
    public static final String DAMAGE_CAUSE_ID = "NakedHyfraid_Armour";

    private final Map<UUID, Float> elapsed = new ConcurrentHashMap<>();
    private DamageCause damageCause;

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getGatherDamageGroup();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(
                PlayerRef.getComponentType(),
                InventoryComponent.Armor.getComponentType(),
                Query.not(DeathComponent.getComponentType()),
                Query.not(Invulnerable.getComponentType())
        );
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        NakedHyfraidConfig config = NakedHyfraidPlugin.get().getConfig();
        if (config.getArmourDamagePerPiece() <= 0.0f) {
            return;
        }
        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        InventoryComponent.Armor armor = archetypeChunk.getComponent(index, InventoryComponent.Armor.getComponentType());
        if (playerRef == null || armor == null) {
            return;
        }

        int pieces = countPieces(armor.getInventory());
        UUID uuid = playerRef.getUuid();
        if (pieces == 0) {
            // Taking armour off resets the timer, so re-equipping can't dodge a tick that was nearly due.
            elapsed.remove(uuid);
            return;
        }

        float time = elapsed.getOrDefault(uuid, 0.0f) + dt;
        float interval = config.getArmourDamageIntervalSeconds();
        if (time < interval) {
            elapsed.put(uuid, time);
            return;
        }
        elapsed.put(uuid, time % interval);

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Damage damage = new Damage(Damage.NULL_SOURCE, getDamageCause(), config.getArmourDamagePerPiece() * pieces);
        DamageSystems.executeDamage(ref, commandBuffer, damage);
    }

    public void forget(UUID uuid) {
        elapsed.remove(uuid);
    }

    private static int countPieces(ItemContainer container) {
        int pieces = 0;
        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            if (!ItemStack.isEmpty(container.getItemStack(slot))) {
                pieces++;
            }
        }
        return pieces;
    }

    private DamageCause getDamageCause() {
        if (damageCause == null) {
            DamageCause fromAssets = DamageCause.getAssetMap().getAsset(DAMAGE_CAUSE_ID);
            // Same fallback the Hungry mod uses; bypasses resistances so armour can't block its own penalty.
            damageCause = fromAssets != null ? fromAssets : new DamageCause(DAMAGE_CAUSE_ID, DAMAGE_CAUSE_ID, false, false, true);
        }
        return damageCause;
    }
}
