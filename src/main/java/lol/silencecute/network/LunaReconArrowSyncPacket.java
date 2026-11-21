package lol.silencecute.network;

import java.util.function.Supplier;

import lol.silencecute.client.LunaReconArrowClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public final class LunaReconArrowSyncPacket {
    private final boolean drawn;
    private final int cooldownMillis;
    private final int charges;
    private final int rechargeMillis;
    private final int sequence;

    public LunaReconArrowSyncPacket(boolean drawn, int cooldownMillis,
                                    int charges, int rechargeMillis, int sequence) {
        this.drawn = drawn;
        this.cooldownMillis = Math.max(0, cooldownMillis);
        this.charges = Math.max(0, charges);
        this.rechargeMillis = Math.max(0, rechargeMillis);
        this.sequence = sequence;
    }

    public LunaReconArrowSyncPacket(FriendlyByteBuf buffer) {
        this.drawn = buffer.readBoolean();
        this.cooldownMillis = buffer.readInt();
        this.charges = buffer.readUnsignedByte();
        this.rechargeMillis = buffer.readInt();
        this.sequence = buffer.readVarInt();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(drawn);
        buffer.writeInt(cooldownMillis);
        buffer.writeByte(charges);
        buffer.writeInt(rechargeMillis);
        buffer.writeVarInt(sequence);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> LunaReconArrowClient.applyServerSync(
                drawn, cooldownMillis, charges, rechargeMillis, sequence));
        context.setPacketHandled(true);
    }
}
