package lol.silencecute.client;

import lol.silencecute.Client;
import lol.silencecute.event.TempstRollSizeHandler;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)
public final class TempstRollClientSizeHandler {

    private TempstRollClientSizeHandler() {
    }

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.level().isClientSide) return;
        if (!TempstRollClient.isRollingFor(player.getId())) return;

        event.setNewSize(EntityDimensions.scalable(
                TempstRollSizeHandler.ROLL_WIDTH, TempstRollSizeHandler.ROLL_HEIGHT));
        event.setNewEyeHeight(TempstRollSizeHandler.ROLL_HEIGHT * 0.85F);
    }
}
