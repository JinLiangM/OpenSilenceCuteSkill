package lol.silencecute.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import lol.silencecute.Client;
import lol.silencecute.client.compat.YsmCompat;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)
public final class TempstRollBodySpin {

    private static final Map<Integer, Boolean> FORCED_CROUCH = new HashMap<>();

    private TempstRollBodySpin() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !event.level.isClientSide) return;

        if (!YsmCompat.useYsmAnimation()) {
            releaseAll(event.level);
            return;
        }

        ClientLevel level = (ClientLevel) event.level;
        Set<Integer> seen = new HashSet<>();

        for (Player player : level.players()) {
            int id = player.getId();
            float progress = TempstRollClient.progressFor(id);
            if (progress < 0.0F || progress >= 1.0F) continue;

            seen.add(id);
            FORCED_CROUCH.put(id, Boolean.TRUE);

            player.setShiftKeyDown(true);
            if (player.getPose() != Pose.CROUCHING) {
                player.setPose(Pose.CROUCHING);
            }
        }

        Iterator<Map.Entry<Integer, Boolean>> it = FORCED_CROUCH.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Boolean> entry = it.next();
            if (seen.contains(entry.getKey())) continue;
            it.remove();
            restore(level.getEntity(entry.getKey()));
        }
    }

    private static void releaseAll(Object level) {
        if (FORCED_CROUCH.isEmpty()) return;
        if (!(level instanceof ClientLevel clientLevel)) return;
        for (Integer id : FORCED_CROUCH.keySet()) {
            restore(clientLevel.getEntity(id));
        }
        FORCED_CROUCH.clear();
    }

    private static void restore(Entity entity) {
        if (!(entity instanceof Player player)) return;
        player.setShiftKeyDown(false);
        if (player.getPose() == Pose.CROUCHING) {
            player.setPose(Pose.STANDING);
            player.refreshDimensions();
        }
    }
}
