package lol.silencecute.network;

import java.util.function.Supplier;

import lol.silencecute.event.OperatorState;
import lol.silencecute.manager.DashService;
import lol.silencecute.manager.TempstRollService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class DashPacket {
    private final float directionX;
    private final float directionZ;

    public DashPacket(float directionX, float directionZ) {
        this.directionX = directionX;
        this.directionZ = directionZ;
    }

    public DashPacket(FriendlyByteBuf buffer) {
        this.directionX = buffer.readFloat();
        this.directionZ = buffer.readFloat();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeFloat(directionX);
        buffer.writeFloat(directionZ);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (OperatorState.isJifeng(player.getUUID())) {
                TempstRollService.start(player, directionX, directionZ);
            } else {
                DashService.start(player, directionX, directionZ);
            }
        });
        context.setPacketHandled(true);
    }
}
