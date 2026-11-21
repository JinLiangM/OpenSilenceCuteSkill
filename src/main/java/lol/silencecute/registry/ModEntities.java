package lol.silencecute.registry;



import lol.silencecute.entity.LunaGrenadeEntity;
import lol.silencecute.entity.LunaReconArrowEntity;
import lol.silencecute.entity.LunaShockArrowEntity;
import lol.silencecute.entity.MagneticBombEntity;
import lol.silencecute.*;

import net.minecraft.world.entity.EntityType;

import net.minecraft.world.entity.MobCategory;

import net.minecraftforge.registries.DeferredRegister;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraftforge.registries.RegistryObject;



public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =

            DeferredRegister.create(ForgeRegistries.Keys.ENTITY_TYPES, Client.MOD_ID);



    public static final RegistryObject<EntityType<MagneticBombEntity>> MAGNETIC_BOMB =

            ENTITY_TYPES.register("magnetic_bomb", () -> EntityType.Builder

                    .<MagneticBombEntity>of(MagneticBombEntity::new, MobCategory.MISC)

                    .sized(0.3F, 0.3F)

                    .clientTrackingRange(128)

                    .updateInterval(1)

                    .setShouldReceiveVelocityUpdates(true)

                    .build(Client.MOD_ID + ":magnetic_bomb"));



    public static final RegistryObject<EntityType<LunaGrenadeEntity>> LUNA_GRENADE =

            ENTITY_TYPES.register("luna_grenade", () -> EntityType.Builder

                    .<LunaGrenadeEntity>of(LunaGrenadeEntity::new, MobCategory.MISC)

                    .sized(0.28F, 0.28F)

                    .clientTrackingRange(96)

                    .updateInterval(1)

                    .setShouldReceiveVelocityUpdates(true)

                    .build(Client.MOD_ID + ":luna_grenade"));



    public static final RegistryObject<EntityType<LunaShockArrowEntity>> LUNA_SHOCK_ARROW =

            ENTITY_TYPES.register("luna_shock_arrow", () -> EntityType.Builder

                    .<LunaShockArrowEntity>of(LunaShockArrowEntity::new, MobCategory.MISC)

                    .sized(0.32F, 0.32F)

                    .clientTrackingRange(128)

                    .updateInterval(1)

                    .setShouldReceiveVelocityUpdates(true)

                    .build(Client.MOD_ID + ":luna_shock_arrow"));

    public static final RegistryObject<EntityType<LunaReconArrowEntity>> LUNA_RECON_ARROW =

            ENTITY_TYPES.register("luna_recon_arrow", () -> EntityType.Builder

                    .<LunaReconArrowEntity>of(LunaReconArrowEntity::new, MobCategory.MISC)

                    .sized(0.32F, 0.32F)

                    .clientTrackingRange(128)

                    .updateInterval(1)

                    .setShouldReceiveVelocityUpdates(true)

                    .build(Client.MOD_ID + ":luna_recon_arrow"));

    private ModEntities() {

    }

}


