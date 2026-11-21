package lol.silencecute.network;

import java.util.function.Supplier;

import lol.silencecute.client.TempstRollFx;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public final class TempstRollSyncPacket {
    private final int entityId;
    private final int ticks;
    private final float dirX;
    private final float dirZ;

    public TempstRollSyncPacket(int entityId, int ticks, float dirX, float dirZ) {
        this.entityId = entityId;
        this.ticks = ticks;
        this.dirX = dirX;
        this.dirZ = dirZ;
    }

    public TempstRollSyncPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readVarInt();
        this.ticks = buffer.readVarInt();
        this.dirX = buffer.readFloat();
        this.dirZ = buffer.readFloat();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeVarInt(ticks);
        buffer.writeFloat(dirX);
        buffer.writeFloat(dirZ);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> TempstRollFx.track(entityId, ticks, dirX, dirZ));
        context.setPacketHandled(true);
    }
}
