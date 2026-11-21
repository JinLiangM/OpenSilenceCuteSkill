package lol.silencecute.config;

import lol.silencecute.entity.LunaGrenadeEntity;
import lol.silencecute.entity.LunaReconArrowEntity;
import lol.silencecute.entity.LunaShockArrowEntity;
import lol.silencecute.entity.MagneticBombEntity;
import lol.silencecute.manager.DashService;
import lol.silencecute.manager.LunaGrenadeService;
import lol.silencecute.manager.LunaReconArrowService;
import lol.silencecute.manager.LunaShockArrowService;
import lol.silencecute.manager.MagneticBombService;
import lol.silencecute.manager.TempstRollService;
import lol.silencecute.manager.TempstUltimateService;

import net.minecraftforge.common.ForgeConfigSpec;

public final class SkillConfigData {

    public static final ForgeConfigSpec SPEC;


    public static final ForgeConfigSpec.IntValue C4_MAX_CHARGES;
    public static final ForgeConfigSpec.DoubleValue C4_RECHARGE_SECONDS;
    public static final ForgeConfigSpec.IntValue C4_MIN_THROW_INTERVAL_TICKS;
    public static final ForgeConfigSpec.DoubleValue C4_THROW_SPEED;
    public static final ForgeConfigSpec.DoubleValue C4_THROW_ARC_DEGREES;
    public static final ForgeConfigSpec.DoubleValue C4_THROW_INACCURACY;
    public static final ForgeConfigSpec.DoubleValue C4_GRAVITY;
    public static final ForgeConfigSpec.DoubleValue C4_FUSE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue C4_AIR_TIMEOUT_SECONDS;
    public static final ForgeConfigSpec.DoubleValue C4_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue C4_RADIUS;
    public static final ForgeConfigSpec.DoubleValue C4_KNOCKBACK_HORIZONTAL;
    public static final ForgeConfigSpec.DoubleValue C4_KNOCKBACK_VERTICAL;


    public static final ForgeConfigSpec.IntValue GRENADE_MAX_CHARGES;
    public static final ForgeConfigSpec.DoubleValue GRENADE_RECHARGE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue GRENADE_FULL_FUSE_SECONDS;
    public static final ForgeConfigSpec.IntValue GRENADE_AUTO_THROW_REMAINING_TICKS;
    public static final ForgeConfigSpec.DoubleValue GRENADE_THROW_SPEED;
    public static final ForgeConfigSpec.DoubleValue GRENADE_THROW_ARC_DEGREES;
    public static final ForgeConfigSpec.DoubleValue GRENADE_RADIUS;


    public static final ForgeConfigSpec.IntValue SHOCK_MAX_CHARGES;
    public static final ForgeConfigSpec.DoubleValue SHOCK_RECHARGE_SECONDS;
    public static final ForgeConfigSpec.IntValue SHOCK_FIRE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue SHOCK_DISCHARGE_SECONDS;
    public static final ForgeConfigSpec.IntValue SHOCK_PULSE_TICKS;
    public static final ForgeConfigSpec.DoubleValue SHOCK_MAX_HEALTH_DAMAGE_RATIO;
    public static final ForgeConfigSpec.DoubleValue SHOCK_MIN_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SHOCK_MAX_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SHOCK_FIELD_RADIUS;
    public static final ForgeConfigSpec.DoubleValue SHOCK_MIN_SPEED;
    public static final ForgeConfigSpec.DoubleValue SHOCK_MAX_SPEED;
    public static final ForgeConfigSpec.DoubleValue SHOCK_TAP_SHOT_MAX_POWER;
    public static final ForgeConfigSpec.DoubleValue SHOCK_TAP_SHOT_HORIZONTAL_SCALE;
    public static final ForgeConfigSpec.DoubleValue SHOCK_TAP_SHOT_VERTICAL_SCALE;
    public static final ForgeConfigSpec.DoubleValue SHOCK_ARROW_GRAVITY;
    public static final ForgeConfigSpec.IntValue SHOCK_ARROW_MAX_BOUNCES;
    public static final ForgeConfigSpec.DoubleValue SHOCK_ARROW_MAX_FLIGHT_SECONDS;
    public static final ForgeConfigSpec.DoubleValue SHOCK_ARROW_BOUNCE_FACTOR;


    public static final ForgeConfigSpec.IntValue RECON_MAX_CHARGES;
    public static final ForgeConfigSpec.DoubleValue RECON_RECHARGE_SECONDS;
    public static final ForgeConfigSpec.IntValue RECON_FIRE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.DoubleValue RECON_MIN_SPEED;
    public static final ForgeConfigSpec.DoubleValue RECON_MAX_SPEED;
    public static final ForgeConfigSpec.DoubleValue RECON_TAP_SHOT_MAX_POWER;
    public static final ForgeConfigSpec.DoubleValue RECON_TAP_SHOT_HORIZONTAL_SCALE;
    public static final ForgeConfigSpec.DoubleValue RECON_TAP_SHOT_VERTICAL_SCALE;
    public static final ForgeConfigSpec.DoubleValue RECON_ARROW_GRAVITY;
    public static final ForgeConfigSpec.DoubleValue RECON_ARROW_MAX_RADIUS;
    public static final ForgeConfigSpec.DoubleValue RECON_ARROW_MIN_LANDED_SECONDS;
    public static final ForgeConfigSpec.DoubleValue RECON_ARROW_MAX_LANDED_SECONDS;


    public static final ForgeConfigSpec.IntValue DASH_TICKS;
    public static final ForgeConfigSpec.DoubleValue DASH_TOTAL_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue DASH_END_SPEED_RATIO;
    public static final ForgeConfigSpec.DoubleValue DASH_TOTAL_RISE;
    public static final ForgeConfigSpec.DoubleValue DASH_STEP_HEIGHT;
    public static final ForgeConfigSpec.DoubleValue DASH_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue DASH_PLAYER_KILL_REFUND;
    public static final ForgeConfigSpec.DoubleValue DASH_ENTITY_KILL_REFUND;


    public static final ForgeConfigSpec.IntValue ROLL_BASE_TICKS;
    public static final ForgeConfigSpec.DoubleValue ROLL_BASE_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue ROLL_PASSIVE_EXTRA_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue ROLL_STAIR_EXTRA_DISTANCE;
    public static final ForgeConfigSpec.DoubleValue ROLL_STAIR_SPEED_MULT;
    public static final ForgeConfigSpec.DoubleValue ROLL_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.DoubleValue ROLL_FALL_DAMAGE_REDUCTION;
    public static final ForgeConfigSpec.IntValue ROLL_LANDING_GRACE_TICKS;
    public static final ForgeConfigSpec.DoubleValue ROLL_PLAYER_KILL_REFUND;
    public static final ForgeConfigSpec.DoubleValue ROLL_ENTITY_KILL_REFUND;


    public static final ForgeConfigSpec.BooleanValue ULT_ENABLED;
    public static final ForgeConfigSpec.DoubleValue ULT_DURATION_SECONDS;
    public static final ForgeConfigSpec.DoubleValue ULT_COOLDOWN_SECONDS;
    public static final ForgeConfigSpec.IntValue ULT_STAND_UP_TICKS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("C4（磁吸炸弹）").push("c4");
        C4_MAX_CHARGES = b.comment("携带充能数")
                .defineInRange("max_charges", MagneticBombService.MAX_CHARGES, 1, 20);
        C4_RECHARGE_SECONDS = b.comment("充能一次需要的秒数")
                .defineInRange("recharge_seconds", seconds(MagneticBombService.RECHARGE_TICKS), 0.0D, 600.0D);
        C4_MIN_THROW_INTERVAL_TICKS = b.comment("两次投掷之间的最小间隔（tick，20 = 1 秒）")
                .defineInRange("min_throw_interval_ticks", MagneticBombService.MIN_THROW_INTERVAL_TICKS, 0, 100);
        C4_THROW_SPEED = b.comment("投掷初速（格/秒）")
                .defineInRange("throw_speed", perSecond(MagneticBombService.THROW_VELOCITY), 0.0D, 200.0D);
        C4_THROW_ARC_DEGREES = b.comment("投掷时额外的俯仰角（度），正数往上抬")
                .defineInRange("throw_arc_degrees", (double) MagneticBombService.THROW_ARC_DEGREES, -89.0D, 89.0D);
        C4_THROW_INACCURACY = b.comment("投掷散布（度），0 = 绝对精准")
                .defineInRange("throw_inaccuracy", (double) MagneticBombService.THROW_INACCURACY, 0.0D, 45.0D);
        C4_GRAVITY = b.comment("飞行重力（格/tick²），越大落得越快、射程越短")
                .defineInRange("gravity", (double) MagneticBombEntity.GRAVITY, 0.0D, 0.5D);
        C4_FUSE_SECONDS = b.comment("吸附后到爆炸的引信时间（秒）")
                .defineInRange("fuse_seconds", seconds(MagneticBombEntity.FUSE_TICKS), 0.05D, 60.0D);
        C4_AIR_TIMEOUT_SECONDS = b.comment("一直没撞到东西时，多少秒后自动消失")
                .defineInRange("air_timeout_seconds", seconds(MagneticBombEntity.AIR_TIMEOUT_TICKS), 1.0D, 600.0D);
        C4_DAMAGE = b.comment("爆心伤害（1 点 = 半颗心，20 点 = 玩家满血）")
                .defineInRange("damage", (double) MagneticBombEntity.MAX_DAMAGE, 0.0D, 1000.0D);
        C4_RADIUS = b.comment("爆炸半径（格），伤害按距离线性衰减到 0")
                .defineInRange("damage_radius", MagneticBombEntity.DAMAGE_RADIUS, 0.0D, 64.0D);
        C4_KNOCKBACK_HORIZONTAL = b.comment("击退水平强度（满伤害时）")
                .defineInRange("knockback_horizontal", MagneticBombEntity.KNOCKBACK_HORIZONTAL, 0.0D, 20.0D);
        C4_KNOCKBACK_VERTICAL = b.comment("击退竖直强度（满伤害时）")
                .defineInRange("knockback_vertical", MagneticBombEntity.KNOCKBACK_VERTICAL, 0.0D, 20.0D);
        b.pop();

        b.comment("露娜：手雷").push("luna_grenade");
        GRENADE_MAX_CHARGES = b.comment("充能数").defineInRange("max_charges", LunaGrenadeService.MAX_CHARGES, 1, 20);
        GRENADE_RECHARGE_SECONDS = b.comment("充能秒数")
                .defineInRange("recharge_seconds", seconds(LunaGrenadeService.RECHARGE_TICKS), 0.0D, 600.0D);
        GRENADE_FULL_FUSE_SECONDS = b.comment("完整引信时长（秒）")
                .defineInRange("full_fuse_seconds", seconds(LunaGrenadeService.FULL_FUSE_TICKS), 0.05D, 60.0D);
        GRENADE_AUTO_THROW_REMAINING_TICKS = b.comment("剩余引信低于这个 tick 数时自动抛出")
                .defineInRange("auto_throw_remaining_ticks", LunaGrenadeService.AUTO_THROW_REMAINING_TICKS, 0, 200);
        GRENADE_THROW_SPEED = b.comment("投掷初速（格/秒）")
                .defineInRange("throw_speed", perSecond(LunaGrenadeService.THROW_VELOCITY), 0.0D, 200.0D);
        GRENADE_THROW_ARC_DEGREES = b.comment("投掷俯仰角（度）")
                .defineInRange("throw_arc_degrees", (double) LunaGrenadeService.THROW_ARC_DEGREES, -89.0D, 89.0D);
        GRENADE_RADIUS = b.comment("爆炸半径（格）")
                .defineInRange("damage_radius", LunaGrenadeEntity.DAMAGE_RADIUS, 0.0D, 64.0D);
        b.pop();

        b.comment("露娜：电箭").push("luna_shock_arrow");
        SHOCK_MAX_CHARGES = b.comment("充能数").defineInRange("max_charges", LunaShockArrowService.MAX_CHARGES, 1, 20);
        SHOCK_RECHARGE_SECONDS = b.comment("充能秒数")
                .defineInRange("recharge_seconds", seconds(LunaShockArrowService.RECHARGE_TICKS), 0.0D, 600.0D);
        SHOCK_FIRE_COOLDOWN_TICKS = b.comment("两次射击的冷却（tick）")
                .defineInRange("fire_cooldown_ticks", LunaShockArrowService.FIRE_COOLDOWN_TICKS, 0, 200);
        SHOCK_DISCHARGE_SECONDS = b.comment("感电持续时间（秒）")
                .defineInRange("discharge_seconds", seconds(LunaShockArrowService.DISCHARGE_TICKS), 0.05D, 120.0D);
        SHOCK_PULSE_TICKS = b.comment("感电期间每隔多少 tick 结算一次伤害")
                .defineInRange("pulse_ticks", LunaShockArrowService.PULSE_TICKS, 1, 200);
        SHOCK_MAX_HEALTH_DAMAGE_RATIO = b.comment("每次结算的最大生命值伤害比例（0.07 = 7%）")
                .defineInRange("max_health_damage_ratio", (double) LunaShockArrowService.MAX_HEALTH_DAMAGE_RATIO, 0.0D, 1.0D);
        SHOCK_MIN_DAMAGE = b.comment("每次结算的最低伤害")
                .defineInRange("min_damage", (double) LunaShockArrowService.MIN_DAMAGE, 0.0D, 100.0D);
        SHOCK_MAX_DAMAGE = b.comment("每次结算的最高伤害")
                .defineInRange("max_damage", (double) LunaShockArrowService.MAX_DAMAGE, 0.0D, 100.0D);
        SHOCK_FIELD_RADIUS = b.comment("电流场半径（格）")
                .defineInRange("field_radius", LunaShockArrowService.FIELD_RADIUS, 0.0D, 64.0D);
        SHOCK_MIN_SPEED = b.comment("最弱蓄力时的飞行速度（格/秒）")
                .defineInRange("min_speed", perSecond(LunaShockArrowService.MIN_SPEED), 0.0D, 200.0D);
        SHOCK_MAX_SPEED = b.comment("满蓄力时的飞行速度（格/秒）")
                .defineInRange("max_speed", perSecond(LunaShockArrowService.MAX_SPEED), 0.0D, 400.0D);
        SHOCK_TAP_SHOT_MAX_POWER = b.comment("点按时视为的最大蓄力（0~1）")
                .defineInRange("tap_shot_max_power", (double) LunaShockArrowService.TAP_SHOT_MAX_POWER, 0.0D, 1.0D);
        SHOCK_TAP_SHOT_HORIZONTAL_SCALE = b.comment("点按抛射的水平速度倍率")
                .defineInRange("tap_shot_horizontal_scale", (double) LunaShockArrowService.TAP_SHOT_HORIZONTAL_SCALE, 0.1D, 5.0D);
        SHOCK_TAP_SHOT_VERTICAL_SCALE = b.comment("点按抛射的竖直速度倍率")
                .defineInRange("tap_shot_vertical_scale", (double) LunaShockArrowService.TAP_SHOT_VERTICAL_SCALE, 0.1D, 5.0D);
        SHOCK_ARROW_GRAVITY = b.comment("箭矢重力（格/tick²）")
                .defineInRange("arrow_gravity", (double) LunaShockArrowEntity.GRAVITY, 0.0D, 0.5D);
        SHOCK_ARROW_MAX_BOUNCES = b.comment("最多反弹次数")
                .defineInRange("arrow_max_bounces", LunaShockArrowEntity.MAX_BOUNCES, 0, 32);
        SHOCK_ARROW_MAX_FLIGHT_SECONDS = b.comment("最长飞行时间（秒），超时自动落地")
                .defineInRange("arrow_max_flight_seconds", seconds(LunaShockArrowEntity.MAX_FLIGHT_TICKS), 0.05D, 120.0D);
        SHOCK_ARROW_BOUNCE_FACTOR = b.comment("反弹速度保留比例")
                .defineInRange("arrow_bounce_factor", (double) LunaShockArrowEntity.BOUNCE_FACTOR, 0.0D, 1.0D);
        b.pop();

        b.comment("露娜：侦察箭").push("luna_recon_arrow");
        RECON_MAX_CHARGES = b.comment("充能数").defineInRange("max_charges", LunaReconArrowService.MAX_CHARGES, 1, 20);
        RECON_RECHARGE_SECONDS = b.comment("充能秒数")
                .defineInRange("recharge_seconds", seconds(LunaReconArrowService.RECHARGE_TICKS), 0.0D, 600.0D);
        RECON_FIRE_COOLDOWN_TICKS = b.comment("两次射击的冷却（tick）")
                .defineInRange("fire_cooldown_ticks", LunaReconArrowService.FIRE_COOLDOWN_TICKS, 0, 200);
        RECON_MIN_SPEED = b.comment("最弱蓄力时的飞行速度（格/秒）")
                .defineInRange("min_speed", perSecond(LunaReconArrowService.MIN_SPEED), 0.0D, 200.0D);
        RECON_MAX_SPEED = b.comment("满蓄力时的飞行速度（格/秒）")
                .defineInRange("max_speed", perSecond(LunaReconArrowService.MAX_SPEED), 0.0D, 400.0D);
        RECON_TAP_SHOT_MAX_POWER = b.comment("点按时视为的最大蓄力（0~1）")
                .defineInRange("tap_shot_max_power", (double) LunaReconArrowService.TAP_SHOT_MAX_POWER, 0.0D, 1.0D);
        RECON_TAP_SHOT_HORIZONTAL_SCALE = b.comment("点按抛射的水平速度倍率")
                .defineInRange("tap_shot_horizontal_scale", (double) LunaReconArrowService.TAP_SHOT_HORIZONTAL_SCALE, 0.1D, 5.0D);
        RECON_TAP_SHOT_VERTICAL_SCALE = b.comment("点按抛射的竖直速度倍率")
                .defineInRange("tap_shot_vertical_scale", (double) LunaReconArrowService.TAP_SHOT_VERTICAL_SCALE, 0.1D, 5.0D);
        RECON_ARROW_GRAVITY = b.comment("箭矢重力（格/tick²）")
                .defineInRange("arrow_gravity", (double) LunaReconArrowEntity.GRAVITY, 0.0D, 0.5D);
        RECON_ARROW_MAX_RADIUS = b.comment("飞行时最大扫描半径（格）")
                .defineInRange("arrow_max_radius", (double) LunaReconArrowEntity.MAX_RADIUS, 0.0D, 128.0D);
        RECON_ARROW_MIN_LANDED_SECONDS = b.comment("落地后至少扫描多久（秒）")
                .defineInRange("arrow_min_landed_seconds", seconds(LunaReconArrowEntity.MIN_LANDED_TICKS), 0.0D, 120.0D);
        RECON_ARROW_MAX_LANDED_SECONDS = b.comment("落地后最多扫描多久（秒）")
                .defineInRange("arrow_max_landed_seconds", seconds(LunaReconArrowEntity.MAX_LANDED_TICKS), 0.0D, 300.0D);
        b.pop();

        b.comment("威龙（Vyron）：冲刺").push("dash");
        DASH_TICKS = b.comment("冲刺持续 tick 数").defineInRange("dash_ticks", DashService.DASH_TICKS, 1, 200);
        DASH_TOTAL_DISTANCE = b.comment("冲刺总距离（格）")
                .defineInRange("total_distance", DashService.TOTAL_DISTANCE, 0.0D, 200.0D);
        DASH_END_SPEED_RATIO = b.comment("冲刺末速相对首速的比例（0.3 = 收尾时只剩 30%）")
                .defineInRange("end_speed_ratio", DashService.END_SPEED_RATIO, 0.05D, 2.0D);
        DASH_TOTAL_RISE = b.comment("冲刺整体抬升高度（格）")
                .defineInRange("total_rise", DashService.TOTAL_RISE, 0.0D, 20.0D);
        DASH_STEP_HEIGHT = b.comment("冲刺期间的自动跨台阶高度（格）")
                .defineInRange("step_height", (double) DashService.DASH_STEP_HEIGHT, 0.0D, 4.0D);
        DASH_COOLDOWN_SECONDS = b.comment("冷却秒数")
                .defineInRange("cooldown_seconds", seconds(DashService.COOLDOWN_TICKS), 0.0D, 900.0D);
        DASH_PLAYER_KILL_REFUND = b.comment("击杀玩家返还的冷却比例（0.9 = 返还 90%）")
                .defineInRange("player_kill_refund", (double) DashService.PLAYER_KILL_REFUND, 0.0D, 1.0D);
        DASH_ENTITY_KILL_REFUND = b.comment("击杀生物返还的冷却比例")
                .defineInRange("entity_kill_refund", (double) DashService.ENTITY_KILL_REFUND, 0.0D, 1.0D);
        b.pop();

        b.comment("疾风（Tempst）：战术翻滚").push("tempst_roll");
        ROLL_BASE_TICKS = b.comment("翻滚持续 tick 数").defineInRange("base_ticks", TempstRollService.BASE_TICKS, 1, 200);
        ROLL_BASE_DISTANCE = b.comment("基础翻滚距离（格）")
                .defineInRange("base_distance", TempstRollService.BASE_DISTANCE, 0.0D, 100.0D);
        ROLL_PASSIVE_EXTRA_DISTANCE = b.comment("被动生效时额外距离（格）")
                .defineInRange("passive_extra_distance", TempstRollService.PASSIVE_EXTRA_DISTANCE, 0.0D, 100.0D);
        ROLL_STAIR_EXTRA_DISTANCE = b.comment("楼梯/台阶上的额外距离（格）")
                .defineInRange("stair_extra_distance", TempstRollService.STAIR_EXTRA_DISTANCE, 0.0D, 100.0D);
        ROLL_STAIR_SPEED_MULT = b.comment("楼梯上的移动速度倍率")
                .defineInRange("stair_speed_mult", TempstRollService.STAIR_SPEED_MULT, 0.1D, 5.0D);
        ROLL_COOLDOWN_SECONDS = b.comment("冷却秒数")
                .defineInRange("cooldown_seconds", seconds(TempstRollService.COOLDOWN_TICKS), 0.0D, 900.0D);
        ROLL_FALL_DAMAGE_REDUCTION = b.comment("落地摔落伤害减免比例（0.8 = 减 80%）")
                .defineInRange("fall_damage_reduction", (double) TempstRollService.FALL_DAMAGE_REDUCTION, 0.0D, 1.0D);
        ROLL_LANDING_GRACE_TICKS = b.comment("翻滚结束后仍享受减伤的 tick 数")
                .defineInRange("landing_grace_ticks", TempstRollService.LANDING_GRACE_TICKS, 0, 200);
        ROLL_PLAYER_KILL_REFUND = b.comment("击杀玩家返还的冷却比例")
                .defineInRange("player_kill_refund", (double) TempstRollService.PLAYER_KILL_REFUND, 0.0D, 1.0D);
        ROLL_ENTITY_KILL_REFUND = b.comment("击杀生物返还的冷却比例")
                .defineInRange("entity_kill_refund", (double) TempstRollService.ENTITY_KILL_REFUND, 0.0D, 1.0D);
        b.pop();

        b.comment("疾风（Tempst）：终极技能").push("tempst_ultimate");
        ULT_ENABLED = b.comment("是否启用（开发中，默认关闭）").define("enabled", TempstUltimateService.ENABLED);
        ULT_DURATION_SECONDS = b.comment("持续秒数")
                .defineInRange("duration_seconds", seconds(TempstUltimateService.DURATION_TICKS), 0.0D, 600.0D);
        ULT_COOLDOWN_SECONDS = b.comment("冷却秒数")
                .defineInRange("cooldown_seconds", seconds(TempstUltimateService.COOLDOWN_TICKS), 0.0D, 1800.0D);
        ULT_STAND_UP_TICKS = b.comment("起身动作 tick 数")
                .defineInRange("stand_up_ticks", TempstUltimateService.STAND_UP_TICKS, 0, 100);
        b.pop();

        SPEC = b.build();
    }

    private SkillConfigData() {
    }


    public static void applyAll() {
        if (!SPEC.isLoaded()) return;

        applyC4();
        applyLunaGrenade();
        applyLunaShockArrow();
        applyLunaReconArrow();
        applyDash();
        applyTempstRoll();
        applyTempstUltimate();
    }

    private static void applyC4() {
        MagneticBombService.MAX_CHARGES = C4_MAX_CHARGES.get();
        MagneticBombService.RECHARGE_TICKS = ticks(C4_RECHARGE_SECONDS.get());
        MagneticBombService.MIN_THROW_INTERVAL_TICKS = C4_MIN_THROW_INTERVAL_TICKS.get();
        MagneticBombService.THROW_VELOCITY = perTick(C4_THROW_SPEED.get());
        MagneticBombService.THROW_ARC_DEGREES = (float) C4_THROW_ARC_DEGREES.get().doubleValue();
        MagneticBombService.THROW_INACCURACY = (float) C4_THROW_INACCURACY.get().doubleValue();

        MagneticBombEntity.GRAVITY = (float) C4_GRAVITY.get().doubleValue();
        MagneticBombEntity.FUSE_TICKS = ticks(C4_FUSE_SECONDS.get());
        MagneticBombEntity.AIR_TIMEOUT_TICKS = ticks(C4_AIR_TIMEOUT_SECONDS.get());
        MagneticBombEntity.MAX_DAMAGE = (float) C4_DAMAGE.get().doubleValue();
        MagneticBombEntity.DAMAGE_RADIUS = C4_RADIUS.get();
        MagneticBombEntity.KNOCKBACK_HORIZONTAL = C4_KNOCKBACK_HORIZONTAL.get();
        MagneticBombEntity.KNOCKBACK_VERTICAL = C4_KNOCKBACK_VERTICAL.get();
    }

    private static void applyLunaGrenade() {
        LunaGrenadeService.MAX_CHARGES = GRENADE_MAX_CHARGES.get();
        LunaGrenadeService.RECHARGE_TICKS = ticks(GRENADE_RECHARGE_SECONDS.get());
        LunaGrenadeService.FULL_FUSE_TICKS = ticks(GRENADE_FULL_FUSE_SECONDS.get());
        LunaGrenadeService.AUTO_THROW_REMAINING_TICKS = GRENADE_AUTO_THROW_REMAINING_TICKS.get();
        LunaGrenadeService.THROW_VELOCITY = perTick(GRENADE_THROW_SPEED.get());
        LunaGrenadeService.THROW_ARC_DEGREES = (float) GRENADE_THROW_ARC_DEGREES.get().doubleValue();
        LunaGrenadeEntity.DAMAGE_RADIUS = GRENADE_RADIUS.get();
    }

    private static void applyLunaShockArrow() {
        LunaShockArrowService.MAX_CHARGES = SHOCK_MAX_CHARGES.get();
        LunaShockArrowService.RECHARGE_TICKS = ticks(SHOCK_RECHARGE_SECONDS.get());
        LunaShockArrowService.FIRE_COOLDOWN_TICKS = SHOCK_FIRE_COOLDOWN_TICKS.get();
        LunaShockArrowService.DISCHARGE_TICKS = ticks(SHOCK_DISCHARGE_SECONDS.get());
        LunaShockArrowService.PULSE_TICKS = SHOCK_PULSE_TICKS.get();
        LunaShockArrowService.MAX_HEALTH_DAMAGE_RATIO = (float) SHOCK_MAX_HEALTH_DAMAGE_RATIO.get().doubleValue();
        LunaShockArrowService.MIN_DAMAGE = (float) SHOCK_MIN_DAMAGE.get().doubleValue();
        LunaShockArrowService.MAX_DAMAGE = (float) SHOCK_MAX_DAMAGE.get().doubleValue();
        LunaShockArrowService.FIELD_RADIUS = SHOCK_FIELD_RADIUS.get();
        LunaShockArrowService.MIN_SPEED = perTick(SHOCK_MIN_SPEED.get());
        LunaShockArrowService.MAX_SPEED = perTick(SHOCK_MAX_SPEED.get());
        LunaShockArrowService.TAP_SHOT_MAX_POWER = (float) SHOCK_TAP_SHOT_MAX_POWER.get().doubleValue();
        LunaShockArrowService.TAP_SHOT_HORIZONTAL_SCALE = (float) SHOCK_TAP_SHOT_HORIZONTAL_SCALE.get().doubleValue();
        LunaShockArrowService.TAP_SHOT_VERTICAL_SCALE = (float) SHOCK_TAP_SHOT_VERTICAL_SCALE.get().doubleValue();

        LunaShockArrowEntity.GRAVITY = (float) SHOCK_ARROW_GRAVITY.get().doubleValue();
        LunaShockArrowEntity.MAX_BOUNCES = SHOCK_ARROW_MAX_BOUNCES.get();
        LunaShockArrowEntity.MAX_FLIGHT_TICKS = ticks(SHOCK_ARROW_MAX_FLIGHT_SECONDS.get());
        LunaShockArrowEntity.BOUNCE_FACTOR = (float) SHOCK_ARROW_BOUNCE_FACTOR.get().doubleValue();
    }

    private static void applyLunaReconArrow() {
        LunaReconArrowService.MAX_CHARGES = RECON_MAX_CHARGES.get();
        LunaReconArrowService.RECHARGE_TICKS = ticks(RECON_RECHARGE_SECONDS.get());
        LunaReconArrowService.FIRE_COOLDOWN_TICKS = RECON_FIRE_COOLDOWN_TICKS.get();
        LunaReconArrowService.MIN_SPEED = perTick(RECON_MIN_SPEED.get());
        LunaReconArrowService.MAX_SPEED = perTick(RECON_MAX_SPEED.get());
        LunaReconArrowService.TAP_SHOT_MAX_POWER = (float) RECON_TAP_SHOT_MAX_POWER.get().doubleValue();
        LunaReconArrowService.TAP_SHOT_HORIZONTAL_SCALE = (float) RECON_TAP_SHOT_HORIZONTAL_SCALE.get().doubleValue();
        LunaReconArrowService.TAP_SHOT_VERTICAL_SCALE = (float) RECON_TAP_SHOT_VERTICAL_SCALE.get().doubleValue();

        LunaReconArrowEntity.GRAVITY = (float) RECON_ARROW_GRAVITY.get().doubleValue();
        LunaReconArrowEntity.MAX_RADIUS = (float) RECON_ARROW_MAX_RADIUS.get().doubleValue();
        LunaReconArrowEntity.MIN_LANDED_TICKS = ticks(RECON_ARROW_MIN_LANDED_SECONDS.get());
        LunaReconArrowEntity.MAX_LANDED_TICKS = ticks(RECON_ARROW_MAX_LANDED_SECONDS.get());
    }

    private static void applyDash() {
        DashService.DASH_TICKS = DASH_TICKS.get();
        DashService.TOTAL_DISTANCE = DASH_TOTAL_DISTANCE.get();
        DashService.END_SPEED_RATIO = DASH_END_SPEED_RATIO.get();
        DashService.TOTAL_RISE = DASH_TOTAL_RISE.get();
        DashService.DASH_STEP_HEIGHT = (float) DASH_STEP_HEIGHT.get().doubleValue();
        DashService.COOLDOWN_TICKS = ticks(DASH_COOLDOWN_SECONDS.get());
        DashService.PLAYER_KILL_REFUND = (float) DASH_PLAYER_KILL_REFUND.get().doubleValue();
        DashService.ENTITY_KILL_REFUND = (float) DASH_ENTITY_KILL_REFUND.get().doubleValue();
        DashService.recomputeDerived();
    }

    private static void applyTempstRoll() {
        TempstRollService.BASE_TICKS = ROLL_BASE_TICKS.get();
        TempstRollService.BASE_DISTANCE = ROLL_BASE_DISTANCE.get();
        TempstRollService.PASSIVE_EXTRA_DISTANCE = ROLL_PASSIVE_EXTRA_DISTANCE.get();
        TempstRollService.STAIR_EXTRA_DISTANCE = ROLL_STAIR_EXTRA_DISTANCE.get();
        TempstRollService.STAIR_SPEED_MULT = ROLL_STAIR_SPEED_MULT.get();
        TempstRollService.COOLDOWN_TICKS = ticks(ROLL_COOLDOWN_SECONDS.get());
        TempstRollService.FALL_DAMAGE_REDUCTION = (float) ROLL_FALL_DAMAGE_REDUCTION.get().doubleValue();
        TempstRollService.LANDING_GRACE_TICKS = ROLL_LANDING_GRACE_TICKS.get();
        TempstRollService.PLAYER_KILL_REFUND = (float) ROLL_PLAYER_KILL_REFUND.get().doubleValue();
        TempstRollService.ENTITY_KILL_REFUND = (float) ROLL_ENTITY_KILL_REFUND.get().doubleValue();
    }

    private static void applyTempstUltimate() {
        TempstUltimateService.ENABLED = ULT_ENABLED.get();
        TempstUltimateService.DURATION_TICKS = ticks(ULT_DURATION_SECONDS.get());
        TempstUltimateService.COOLDOWN_TICKS = ticks(ULT_COOLDOWN_SECONDS.get());
        TempstUltimateService.STAND_UP_TICKS = ULT_STAND_UP_TICKS.get();
    }


    private static double seconds(int ticks) {
        return ticks / 20.0D;
    }

    private static int ticks(double seconds) {
        return Math.max(0, (int) Math.round(seconds * 20.0D));
    }

    private static double perSecond(float perTick) {
        return (double) perTick * 20.0D;
    }

    private static float perTick(double perSecond) {
        return (float) (perSecond / 20.0D);
    }
}
