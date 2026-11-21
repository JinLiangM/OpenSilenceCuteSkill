package lol.silencecute.network;

import java.util.function.Supplier;

import lol.silencecute.client.LunaGrenadeClient;
import lol.silencecute.manager.LunaGrenadeService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public final class LunaGrenadeSyncPacket {
    private final byte state;
    private final int fuseTicks;
    private final int charges;
    private final int rechargeMillis;

    public LunaGrenadeSyncPacket(LunaGrenadeService.HeldState state, int fuseTicks,
                                 int charges, int rechargeMillis) {
        this.state = (byte) state.ordinal();
        this.fuseTicks = Math.max(0, fuseTicks);
        this.charges = Math.max(0, charges);
        this.rechargeMillis = Math.max(0, rechargeMillis);
    }

    public LunaGrenadeSyncPacket(FriendlyByteBuf buffer) {
        this.state = buffer.readByte();
        this.fuseTicks = buffer.readUnsignedShort();
        this.charges = buffer.readUnsignedByte();
        this.rechargeMillis = buffer.readInt();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeByte(state);
        buffer.writeShort(fuseTicks);
        buffer.writeByte(charges);
        buffer.writeInt(rechargeMillis);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> LunaGrenadeClient.applyServerSync(
                state, fuseTicks, charges, rechargeMillis));
        context.setPacketHandled(true);
    }
}
