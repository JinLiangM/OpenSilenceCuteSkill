package lol.silencecute.network;

import java.util.function.Supplier;

import lol.silencecute.client.LunaRevealClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public final class LunaRevealPacket {
    private final int entityId;
    private final int durationTicks;

    public LunaRevealPacket(int entityId, int durationTicks) {
        this.entityId = entityId;
        this.durationTicks = durationTicks;
    }

    public LunaRevealPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readVarInt();
        this.durationTicks = buffer.readVarInt();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeVarInt(durationTicks);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> LunaRevealClient.reveal(entityId, durationTicks));
        context.setPacketHandled(true);
    }
}
