package com.kingtalon23.nakedhyfraid;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Settings saved to the plugin's data folder as {@code config.json}.
 */
public class NakedHyfraidConfig {
    public static final BuilderCodec<NakedHyfraidConfig> CODEC = BuilderCodec.builder(NakedHyfraidConfig.class, NakedHyfraidConfig::new)
            .append(new KeyedCodec<>("Hardcore", Codec.BOOLEAN),
                    (config, value) -> config.hardcore = value,
                    config -> config.hardcore).add()
            .append(new KeyedCodec<>("DeathKickDelaySeconds", Codec.FLOAT),
                    (config, value) -> config.deathKickDelaySeconds = value,
                    config -> config.deathKickDelaySeconds).add()
            .append(new KeyedCodec<>("DisableWorldMap", Codec.BOOLEAN),
                    (config, value) -> config.disableWorldMap = value,
                    config -> config.disableWorldMap).add()
            .append(new KeyedCodec<>("ArmourDamagePerPiece", Codec.FLOAT),
                    (config, value) -> config.armourDamagePerPiece = value,
                    config -> config.armourDamagePerPiece).add()
            .append(new KeyedCodec<>("ArmourDamageIntervalSeconds", Codec.FLOAT),
                    (config, value) -> config.armourDamageIntervalSeconds = value,
                    config -> config.armourDamageIntervalSeconds).add()
            .append(new KeyedCodec<>("DisableFoodHealing", Codec.BOOLEAN),
                    (config, value) -> config.disableFoodHealing = value,
                    config -> config.disableFoodHealing).add()
            .append(new KeyedCodec<>("FoodEffectPrefixes", Codec.STRING_ARRAY),
                    (config, value) -> config.foodEffectPrefixes = value,
                    config -> config.foodEffectPrefixes).add()
            .build();

    private boolean hardcore = true;
    private float deathKickDelaySeconds = 3.0f;
    private boolean disableWorldMap = true;
    private float armourDamagePerPiece = 1.0f;
    private float armourDamageIntervalSeconds = 4.0f;
    private boolean disableFoodHealing = true;
    private String[] foodEffectPrefixes = {"Food_"};

    public boolean isHardcore() {
        return hardcore;
    }

    public float getDeathKickDelaySeconds() {
        return Math.max(0.0f, deathKickDelaySeconds);
    }

    public boolean isDisableWorldMap() {
        return disableWorldMap;
    }

    public float getArmourDamagePerPiece() {
        return armourDamagePerPiece;
    }

    public float getArmourDamageIntervalSeconds() {
        return Math.max(0.05f, armourDamageIntervalSeconds);
    }

    public boolean isDisableFoodHealing() {
        return disableFoodHealing;
    }

    public String[] getFoodEffectPrefixes() {
        return foodEffectPrefixes != null ? foodEffectPrefixes : new String[0];
    }
}
