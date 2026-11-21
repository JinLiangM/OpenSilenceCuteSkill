package lol.silencecute.registry;

import java.util.List;

import lol.silencecute.Client;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.Keys.SOUND_EVENTS, Client.MOD_ID);

    public static final RegistryObject<SoundEvent> WEILONG_DASH = fixed("weilong_dash", 32.0F);
    public static final RegistryObject<SoundEvent> WEILONG_PASSIVE = fixed("weilong_passive", 32.0F);
    public static final RegistryObject<SoundEvent> TEMPST_PASSIVE = fixed("tempst_passive", 32.0F);
    public static final RegistryObject<SoundEvent> TEMPST_DASH = fixed("tempst_dash", 32.0F);
    public static final RegistryObject<SoundEvent> CANT_USE_SKILL = variable("cant_use_skill");
    public static final RegistryObject<SoundEvent> TEMPST_ULT_START = fixed("tempst_ult_start", 64.0F);
    public static final RegistryObject<SoundEvent> SKILL_READY = variable("skill_ready");
    public static final RegistryObject<SoundEvent> LUNA_ENEMY_ANALYSIS_WHERE =
            variable("luna_enemy_analysis_where");
    public static final RegistryObject<SoundEvent> LUNA_ENEMY_ANALYSIS_WATCHING =
            variable("luna_enemy_analysis_watching");
    public static final RegistryObject<SoundEvent> MAGNETIC_BOMB_EXPLOSION =
            fixed("magnetic_bomb_explosion", 600.0F);

    public static final RegistryObject<SoundEvent> C4_BOOM = fixed("c4_boom", 600.0F);
    public static final RegistryObject<SoundEvent> C4_READY = fixed("c4_ready", 96.0F);
    public static final RegistryObject<SoundEvent> C4_IN_HAND = variable("c4_in_hand");

    public static final RegistryObject<SoundEvent> LUNA_GRENADE_PIN = variable("luna_grenade_pin");
    public static final RegistryObject<SoundEvent> LUNA_GRENADE_THROW = variable("luna_grenade_throw");
    public static final RegistryObject<SoundEvent> LUNA_GRENADE_EXPLOSION =
            fixed("luna_grenade_explosion", 96.0F);
    public static final RegistryObject<SoundEvent> LUNA_BIG_SOUND =
            fixed("lunabigsound", 64.0F);
    public static final RegistryObject<SoundEvent> LUNA_GRENADE_VOICE_1 =
            variable("luna_grenade_voice_1");
    public static final RegistryObject<SoundEvent> LUNA_GRENADE_VOICE_2 =
            variable("luna_grenade_voice_2");
    public static final RegistryObject<SoundEvent> LUNA_GRENADE_VOICE_3 =
            variable("luna_grenade_voice_3");
    public static final RegistryObject<SoundEvent> LUNA_GRENADE_VOICE_4 =
            variable("luna_grenade_voice_4");
    public static final RegistryObject<SoundEvent> LUNA_GRENADE_VOICE_5 =
            variable("luna_grenade_voice_5");
    public static final RegistryObject<SoundEvent> LUNA_GRENADE_VOICE_6 =
            variable("luna_grenade_voice_6");

    public static final RegistryObject<SoundEvent> LUNA_RECON_SHOOT_1 = variable("detecting");
    public static final RegistryObject<SoundEvent> LUNA_RECON_SHOOT_2 = variable("let_me_detect_first");
    public static final RegistryObject<SoundEvent> LUNA_RECON_SHOOT_3 = variable("no_secrets_under_my_arrow");
    public static final RegistryObject<SoundEvent> LUNA_RECON_SHOOT_4 = variable("wait_for_my_intel");
    public static final RegistryObject<SoundEvent> LUNA_RECON_SHOOT_5 = variable("where_are_they_hiding");

    public static final RegistryObject<SoundEvent> LUNA_RECON_ARMOR_1 = variable("armored_combatant_identified");
    public static final RegistryObject<SoundEvent> LUNA_RECON_ARMOR_2 = variable("heavily_armored_enemy_detected");
    public static final RegistryObject<SoundEvent> LUNA_RECON_ARMOR_3 = variable("heavily_armored");

    public static final RegistryObject<SoundEvent> LUNA_RECON_FOUND_1 = variable("enemy_located");
    public static final RegistryObject<SoundEvent> LUNA_RECON_FOUND_2 = variable("enemy_right_there");
    public static final RegistryObject<SoundEvent> LUNA_RECON_FOUND_3 = variable("find_them");
    public static final RegistryObject<SoundEvent> LUNA_RECON_FOUND_4 = variable("found_them");
    public static final RegistryObject<SoundEvent> LUNA_RECON_FOUND_5 = variable("hostile_detected");
    public static final RegistryObject<SoundEvent> LUNA_RECON_FOUND_6 = variable("hostiles_detected");
    public static final RegistryObject<SoundEvent> LUNA_RECON_FOUND_7 = variable("no_more_hiding");
    public static final RegistryObject<SoundEvent> LUNA_RECON_FOUND_8 = variable("target_located");

    public static final RegistryObject<SoundEvent> LUNA_BOW_DRAW = variable("luna_bow_draw");
    public static final RegistryObject<SoundEvent> LUNA_BOW_BOUNCE = variable("luna_bow_bounce");
    public static final RegistryObject<SoundEvent> LUNA_BOW_SHOOT = fixed("luna_bow_shoot", 32.0F);
    public static final RegistryObject<SoundEvent> LUNA_SHOCK_DISCHARGE =
            fixed("luna_shock_discharge", 64.0F);

    public static final RegistryObject<SoundEvent> LUNA_SHOCK_VOICE_1 =
            variable("luna_shock_voice_1");
    public static final RegistryObject<SoundEvent> LUNA_SHOCK_VOICE_2 =
            variable("luna_shock_voice_2");
    public static final RegistryObject<SoundEvent> LUNA_SHOCK_VOICE_3 =
            variable("luna_shock_voice_3");
    public static final RegistryObject<SoundEvent> LUNA_SHOCK_VOICE_4 =
            variable("luna_shock_voice_4");

    public static final RegistryObject<SoundEvent> LUNA_PASSIVE_HIT =
            variable("luna_passive_hit");

    private static final List<RegistryObject<SoundEvent>> LUNA_GRENADE_VOICES = List.of(
            LUNA_GRENADE_VOICE_1, LUNA_GRENADE_VOICE_2, LUNA_GRENADE_VOICE_3,
            LUNA_GRENADE_VOICE_4, LUNA_GRENADE_VOICE_5, LUNA_GRENADE_VOICE_6);

    private static final List<RegistryObject<SoundEvent>> LUNA_SHOCK_VOICES = List.of(
            LUNA_SHOCK_VOICE_1, LUNA_SHOCK_VOICE_2, LUNA_SHOCK_VOICE_3, LUNA_SHOCK_VOICE_4);

    private ModSounds() {
    }

    public static SoundEvent lunaGrenadeVoice(int index) {
        return LUNA_GRENADE_VOICES.get(Math.max(0,
                Math.min(LUNA_GRENADE_VOICES.size() - 1, index))).get();
    }

    public static SoundEvent lunaShockVoice(int index) {
        return LUNA_SHOCK_VOICES.get(Math.max(0,
                Math.min(LUNA_SHOCK_VOICES.size() - 1, index))).get();
    }

    private static RegistryObject<SoundEvent> fixed(String name, float range) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createFixedRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Client.MOD_ID, name), range));
    }

    private static RegistryObject<SoundEvent> variable(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Client.MOD_ID, name)));
    }
}
