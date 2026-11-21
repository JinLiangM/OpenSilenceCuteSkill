package lol.silencecute.command;

import java.util.UUID;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;

import lol.silencecute.access.AccessService;
import lol.silencecute.api.Operator;
import lol.silencecute.network.LunaRevealPacket;
import lol.silencecute.registry.ModNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

public final class AdminCommands {

    private AdminCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(build("Admin"));
        event.getDispatcher().register(build("admin"));
    }

    private static boolean isAdminSource(CommandSourceStack source) {
        if (source.hasPermission(Commands.LEVEL_ADMINS)) return true;
        return source.getEntity() instanceof ServerPlayer player && AccessService.isAdmin(player);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
        return Commands.literal(name)
                .requires(AdminCommands::isAdminSource)
                .then(Commands.literal("TestPlayer")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.literal("true")
                                        .executes(ctx -> setCanChoose(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"), true)))
                                .then(Commands.literal("false")
                                        .executes(ctx -> setCanChoose(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"), false)))))
                .then(Commands.literal("setSkill")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("operator", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                new String[]{"露娜", "威龙", "疾风", "Luna", "Vyron", "Tempst"},
                                                builder))
                                        .executes(ctx -> setSkill(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"),
                                                StringArgumentType.getString(ctx, "operator"))))))
                .then(Commands.literal("CheckPlayer")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> checkPlayer(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("TestModule")
                        .then(Commands.literal("true")
                                .executes(ctx -> setNoCooldown(ctx.getSource(), true)))
                        .then(Commands.literal("false")
                                .executes(ctx -> setNoCooldown(ctx.getSource(), false)))
                        .then(Commands.literal("LUNA1")
                                .executes(ctx -> triggerLunaTest(ctx.getSource()))))
                .then(Commands.literal("TestHUD")
                        .then(Commands.literal("lunaHUD").executes(ctx -> triggerHud(ctx.getSource(), -1)))
                        .then(Commands.literal("lunaHUD2").executes(ctx -> triggerHud(ctx.getSource(), -2))));
    }


    private static int setCanChoose(CommandSourceStack source, ServerPlayer target, boolean allowed) {
        AccessService.setCanChoose(target, allowed);
        String name = target.getGameProfile().getName();
        String operator = AccessService.operatorOf(target.server, target.getUUID()).zhName();
        source.sendSuccess(() -> Component.literal(allowed
                ? "已允许 " + name + " 自定义干员（当前：" + operator + "）"
                : "已锁定 " + name + " 的干员：不能自己切换，固定为 " + operator), true);
        return 1;
    }

    private static int setSkill(CommandSourceStack source, ServerPlayer target, String raw) {
        Operator operator = Operator.byInput(raw);
        if (operator == null || operator == Operator.NONE) {
            source.sendFailure(Component.literal("未知干员：" + raw + "（可用：露娜 / 威龙 / 疾风）"));
            return 0;
        }
        AccessService.setOperator(target, operator);
        source.sendSuccess(() -> Component.literal("已把 " + target.getGameProfile().getName()
                + " 的干员设置为 " + operator.zhName()), true);
        return 1;
    }

    private static int checkPlayer(CommandSourceStack source, ServerPlayer target) {
        String name = target.getGameProfile().getName();
        Operator operator = AccessService.operatorOf(target.server, target.getUUID());
        boolean admin = AccessService.isAdmin(target);
        boolean canChoose = AccessService.canChoose(target);
        boolean explicit = AccessService.isExplicit(target);
        boolean noCooldown = AccessService.isNoCooldown(target.getUUID());

        String switchSource = explicit ? "管理员设置" : (admin ? "OP 权限" : "服务器默认");

        String text = "§e[干员查询] §f" + name + "\n"
                + "§7干员：§f" + operator.zhName() + " §7(" + operator.name() + ")\n"
                + "§7权限组：§f" + (admin ? "管理员" : "普通玩家") + "\n"
                + "§7干员切换：§f" + (canChoose ? "允许" : "不允许")
                + " §7来源：" + switchSource + "\n"
                + "§7技能冷却：§f" + (noCooldown ? "无冷却" : "冷却");
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int setNoCooldown(CommandSourceStack source, boolean enabled) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("此指令只能由玩家执行"));
            return 0;
        }
        boolean result = AccessService.setNoCooldown(player, enabled);
        if (enabled && !result) {
            source.sendFailure(Component.literal("权限不足：普通玩家不能使用无CD技能"));
            return 0;
        }
        player.sendSystemMessage(Component.literal(result
                ? "Admin Test Done!：Disable all cooldowns"
                : "Admin Test Disabled!：Reset all cooldowns"));
        return 1;
    }


    private static final int HUD2_TICK = 40;

    private static final int SAMPLE_START_TICK = 87;

    private static final int CHECK_TICK = 97;

    private static final double MOVE_THRESHOLD_SQR = 0.0001D;

    private static int triggerHud(CommandSourceStack source, int id) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new LunaRevealPacket(id, 0));
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("必须由玩家执行此指令"));
            return 0;
        }
    }

    private static int triggerLunaTest(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            MinecraftForge.EVENT_BUS.register(new LunaTestTracker(player));
            source.sendSuccess(() -> Component.literal("已触发露娜侦察箭 UI 机制测试"), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("此指令只能由玩家执行"));
            return 0;
        }
    }

    private static final class LunaTestTracker {

        private final ServerPlayer player;

        private final UUID playerId;

        private int tickCount;

        private double lastX = Double.NaN;

        private double lastZ = Double.NaN;

        private boolean moved;

        private LunaTestTracker(ServerPlayer player) {
            this.player = player;
            this.playerId = player.getUUID();
        }

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (!player.isAlive() || player.hasDisconnected()) {
                MinecraftForge.EVENT_BUS.unregister(this);
                return;
            }
            tickCount++;
            if (tickCount == 1) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new LunaRevealPacket(-1, 40));
            }
            if (tickCount == HUD2_TICK) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new LunaRevealPacket(-2, 0));
                LogUtils.getLogger().info("[LunaTest] t={} 发黄条 ({})", tickCount, playerId);
            }
            if (tickCount == SAMPLE_START_TICK) {
                lastX = player.getX();
                lastZ = player.getZ();
                LogUtils.getLogger().info("[LunaTest] t={} 开始采样 base=({}, {})", tickCount, lastX, lastZ);
            }
            if (tickCount > SAMPLE_START_TICK && tickCount < CHECK_TICK && !Double.isNaN(lastX)) {
                double dx = player.getX() - lastX;
                double dz = player.getZ() - lastZ;
                if (dx * dx + dz * dz > MOVE_THRESHOLD_SQR) moved = true;
                if (player.zza != 0.0F || player.xxa != 0.0F) moved = true;
                lastX = player.getX();
                lastZ = player.getZ();
            }
            if (tickCount >= CHECK_TICK) {
                LogUtils.getLogger().info("[LunaTest] t={} 判定 moved={} zza={} xxa={}",
                        tickCount, moved, player.zza, player.xxa);
                if (moved) {
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                            new LunaRevealPacket(-1, 40));
                }
                MinecraftForge.EVENT_BUS.unregister(this);
            }
        }
    }
}
