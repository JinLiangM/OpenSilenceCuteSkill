package lol.silencecute.event;

import lol.silencecute.manager.TempstRollService;

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TempstRollSizeHandler {

    public static final float ROLL_WIDTH = 0.6F;
    public static final float ROLL_HEIGHT = 1.0F;

    private TempstRollSizeHandler() {
    }

    @SubscribeEvent
    public static void onEntitySize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!TempstRollService.isRolling(player.getUUID())) return;

        event.setNewSize(EntityDimensions.scalable(ROLL_WIDTH, ROLL_HEIGHT));
        event.setNewEyeHeight(ROLL_HEIGHT * 0.85F);
    }
}
