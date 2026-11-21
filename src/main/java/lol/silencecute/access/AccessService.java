package lol.silencecute.access;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lol.silencecute.api.Operator;
import lol.silencecute.event.OperatorState;
import lol.silencecute.event.PassiveService;
import lol.silencecute.manager.DashService;
import lol.silencecute.manager.LunaGrenadeService;
import lol.silencecute.manager.LunaReconArrowService;
import lol.silencecute.manager.LunaShockArrowService;
import lol.silencecute.manager.MagneticBombService;
import lol.silencecute.manager.TempstRollService;
import lol.silencecute.manager.TempstUltimateService;
import lol.silencecute.network.AccessSyncPacket;
import lol.silencecute.registry.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

public final class AccessService {

    public static final Operator DEFAULT_OPERATOR = Operator.LUNA;

    private static final int ADMIN_PERMISSION_LEVEL = 2;

    private static final Set<UUID> NO_COOLDOWN = new HashSet<>();

    private static final Map<UUID, Integer> LAST_SIGNATURE = new HashMap<>();

    private AccessService() {
    }


    private static AccessData data(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(AccessData::load, AccessData::new, AccessData.DATA_NAME);
    }

    private static AccessData data(ServerPlayer player) {
        return data(player.server);
    }


    public static boolean isAdmin(ServerPlayer player) {
        if (player == null || player.server == null) return false;
        if (player.hasPermissions(ADMIN_PERMISSION_LEVEL)) return true;
        try {
            return player.server.isSingleplayerOwner(player.getGameProfile());
        } catch (Exception ignored) {
            return false;
        }
    }

    public static Operator operatorOf(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) return DEFAULT_OPERATOR;
        Operator stored = data(server).operators.get(uuid);
        return stored == null || stored == Operator.NONE ? DEFAULT_OPERATOR : stored;
    }

    public static boolean isExplicit(ServerPlayer player) {
        if (player == null || player.server == null) return false;
        return data(player).canChoose.containsKey(player.getUUID());
    }

    public static boolean canChoose(ServerPlayer player) {
        if (player == null || player.server == null) return false;
        Boolean explicit = data(player).canChoose.get(player.getUUID());
        if (explicit != null) return explicit;
        return isAdmin(player);
    }

    public static boolean isNoCooldown(UUID uuid) {
        return uuid != null && NO_COOLDOWN.contains(uuid);
    }


    public static void setCanChoose(ServerPlayer target, boolean allowed) {
        if (target == null) return;
        AccessData data = data(target);
        if (!allowed) {
            data.operators.put(target.getUUID(), operatorOf(target.server, target.getUUID()));
        }
        data.canChoose.put(target.getUUID(), allowed);
        data.setDirty();
        sync(target);
    }

    public static void setOperator(ServerPlayer target, Operator operator) {
        if (target == null || operator == null || operator == Operator.NONE) return;
        AccessData data = data(target);
        data.operators.put(target.getUUID(), operator);
        data.setDirty();
        sync(target);
    }

    public static boolean setNoCooldown(ServerPlayer player, boolean enabled) {
        if (player == null) return false;
        UUID uuid = player.getUUID();
        if (enabled && !isAdmin(player)) {
            enabled = false;
        }
        if (enabled) {
            NO_COOLDOWN.add(uuid);
        } else {
            NO_COOLDOWN.remove(uuid);
        }
        clearAllCooldowns(player);
        AccessData data = data(player);
        data.noCooldown.clear();
        data.noCooldown.addAll(NO_COOLDOWN);
        data.setDirty();
        sync(player);
        return enabled;
    }

    public static void clearAllCooldowns(ServerPlayer player) {
        if (player == null) return;
        UUID uuid = player.getUUID();
        DashService.clearCooldown(uuid);
        PassiveService.clearCooldown(uuid);
        LunaGrenadeService.clearCooldown(uuid);
        LunaReconArrowService.clearCooldown(uuid);
        LunaShockArrowService.clearCooldown(uuid);
        MagneticBombService.clearCooldown(uuid);
        TempstRollService.clearCooldown(uuid);
        TempstUltimateService.clearCooldown(uuid);
        DashService.syncCooldown(player);
        LunaGrenadeService.sync(player);
        LunaReconArrowService.sync(player);
        LunaShockArrowService.sync(player);
    }


    public static void sync(ServerPlayer player) {
        if (player == null || player.server == null) return;
        UUID uuid = player.getUUID();
        Operator operator = operatorOf(player.server, uuid);
        OperatorState.set(uuid, operator);
        LAST_SIGNATURE.put(uuid, signature(player, operator));

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new AccessSyncPacket(operator, canChoose(player), isAdmin(player),
                        isExplicit(player), NO_COOLDOWN.contains(uuid)));
    }

    private static int signature(ServerPlayer player, Operator operator) {
        UUID uuid = player.getUUID();
        int sig = operator.ordinal() * 16;
        if (canChoose(player)) sig |= 1;
        if (isAdmin(player)) sig |= 2;
        if (isExplicit(player)) sig |= 4;
        if (NO_COOLDOWN.contains(uuid)) sig |= 8;
        return sig;
    }


    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        AccessData data = data(player);
        if (!data.operators.containsKey(player.getUUID())) {
            data.operators.put(player.getUUID(), DEFAULT_OPERATOR);
            data.setDirty();
        }
        if (data.noCooldown.contains(player.getUUID())) {
            NO_COOLDOWN.add(player.getUUID());
        }
        if (!isAdmin(player) && NO_COOLDOWN.contains(player.getUUID())) {
            NO_COOLDOWN.remove(player.getUUID());
            data.noCooldown.remove(player.getUUID());
            data.setDirty();
        }
        sync(player);
        DashService.syncCooldown(player);
        LunaGrenadeService.sync(player);
        if (NO_COOLDOWN.contains(player.getUUID())) {
            clearAllCooldowns(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SIGNATURE.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.hasDisconnected()) return;

        UUID uuid = player.getUUID();

        if (NO_COOLDOWN.contains(uuid) && !isAdmin(player)) {
            setNoCooldown(player, false);
            player.sendSystemMessage(Component.literal(
                    "§c[三角洲干员] 你的权限组是普通玩家，无法使用无冷却技能，已自动恢复技能冷却。"));
            return;
        }

        if (player.tickCount % 20 != 0) return;
        Operator operator = operatorOf(player.server, uuid);
        int sig = signature(player, operator);
        Integer last = LAST_SIGNATURE.get(uuid);
        if (last == null || last != sig) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        NO_COOLDOWN.clear();
        LAST_SIGNATURE.clear();
    }
}
