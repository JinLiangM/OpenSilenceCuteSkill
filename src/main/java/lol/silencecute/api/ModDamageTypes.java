package lol.silencecute.api;

import java.util.Optional;

import lol.silencecute.Client;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class ModDamageTypes {
    public static final ResourceKey<DamageType> LUNA_EXPLOSION = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(
                    Client.MOD_ID, "luna_explosion"));

    
    static final ResourceKey<DamageType> LUNA_SHOCK = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(
                    Client.MOD_ID, "luna_shock"));

    private ModDamageTypes() {
    }

    
    public static DamageSource shock(Level level, Entity directEntity, Entity causingEntity) {
        Optional<Holder.Reference<DamageType>> custom = holder(level, LUNA_SHOCK);
        if (custom.isPresent()) {
            return new DamageSource(custom.get(), directEntity, causingEntity);
        }

        return vanilla(level, DamageTypes.MAGIC, directEntity, causingEntity);
    }

    
    public static DamageSource explosion(Level level, Entity directEntity, Entity causingEntity) {
        Optional<Holder.Reference<DamageType>> custom = holder(level, LUNA_EXPLOSION);
        if (custom.isPresent()) {
            return new DamageSource(custom.get(), directEntity, causingEntity);
        }
        return level.damageSources().explosion(directEntity, causingEntity);
    }

    private static Optional<Holder.Reference<DamageType>> holder(Level level,
                                                                 ResourceKey<DamageType> type) {
        return level.registryAccess().registry(Registries.DAMAGE_TYPE)
                .flatMap(registry -> registry.getHolder(type));
    }

    
    private static DamageSource vanilla(Level level, ResourceKey<DamageType> type,
                                        Entity directEntity, Entity causingEntity) {
        Optional<Holder.Reference<DamageType>> holder = holder(level, type);
        if (holder.isPresent()) {
            return new DamageSource(holder.get(), directEntity, causingEntity);
        }
        return level.damageSources().magic();
    }
}
