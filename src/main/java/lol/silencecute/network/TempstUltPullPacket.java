package lol.silencecute.network;

import java.util.function.Supplier;

import lol.silencecute.client.TempstUltAnchorClient;
import lol.silencecute.client.TempstUltPullLock;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

public final class TempstUltPullPacket {

    private final boolean pulling;
    private final float yaw;
    private final float pitch;

    public TempstUltPullPacket(boolean pulling, float yaw, float pitch) {
        this.pulling = pulling;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public TempstUltPullPacket(FriendlyByteBuf buffer) {
        this.pulling = buffer.readBoolean();
        this.yaw = buffer.readFloat();
        this.pitch = buffer.readFloat();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(pulling);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide() != LogicalSide.CLIENT) return;
            if (pulling) {
                TempstUltAnchorClient.reset();
                TempstUltPullLock.begin(yaw, pitch);
            } else {
                TempstUltPullLock.end();
            }
        });
        context.setPacketHandled(true);
    }
}
