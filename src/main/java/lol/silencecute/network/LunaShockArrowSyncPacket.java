package lol.silencecute.network;

import java.util.function.Supplier;

import lol.silencecute.client.LunaShockArrowClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public final class LunaShockArrowSyncPacket {
    private final boolean drawn;
    private final boolean bounce;
    private final int cooldownMillis;
    private final int charges;
    private final int rechargeMillis;
    private final int sequence;

    public LunaShockArrowSyncPacket(boolean drawn, boolean bounce, int cooldownMillis,
                                    int charges, int rechargeMillis, int sequence) {
        this.drawn = drawn;
        this.bounce = bounce;
        this.cooldownMillis = Math.max(0, cooldownMillis);
        this.charges = Math.max(0, charges);
        this.rechargeMillis = Math.max(0, rechargeMillis);
        this.sequence = sequence;
    }

    public LunaShockArrowSyncPacket(FriendlyByteBuf buffer) {
        this.drawn = buffer.readBoolean();
        this.bounce = buffer.readBoolean();
        this.cooldownMillis = buffer.readInt();
        this.charges = buffer.readUnsignedByte();
        this.rechargeMillis = buffer.readInt();
        this.sequence = buffer.readVarInt();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeBoolean(drawn);
        buffer.writeBoolean(bounce);
        buffer.writeInt(cooldownMillis);
        buffer.writeByte(charges);
        buffer.writeInt(rechargeMillis);
        buffer.writeVarInt(sequence);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> LunaShockArrowClient.applyServerSync(
                drawn, bounce, cooldownMillis, charges, rechargeMillis, sequence));
        context.setPacketHandled(true);
    }
}
