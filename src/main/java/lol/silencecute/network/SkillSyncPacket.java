package lol.silencecute.network;

import java.util.function.Supplier;

import lol.silencecute.client.ClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public final class SkillSyncPacket {
    private final int dashCooldownMillis;
    private final int passiveCooldownMillis;
    private final int bombCharges;
    private final int bombRechargeMillis;
    private final int ultCooldownMillis;

    public SkillSyncPacket(int dashCooldownMillis, int passiveCooldownMillis,
                           int bombCharges, int bombRechargeMillis, int ultCooldownMillis) {
        this.dashCooldownMillis = dashCooldownMillis;
        this.passiveCooldownMillis = passiveCooldownMillis;
        this.bombCharges = bombCharges;
        this.bombRechargeMillis = bombRechargeMillis;
        this.ultCooldownMillis = ultCooldownMillis;
    }

    public SkillSyncPacket(FriendlyByteBuf buffer) {
        this.dashCooldownMillis = buffer.readInt();
        this.passiveCooldownMillis = buffer.readInt();
        this.bombCharges = buffer.readUnsignedByte();
        this.bombRechargeMillis = buffer.readInt();
        this.ultCooldownMillis = buffer.readInt();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeInt(dashCooldownMillis);
        buffer.writeInt(passiveCooldownMillis);
        buffer.writeByte(bombCharges);
        buffer.writeInt(bombRechargeMillis);
        buffer.writeInt(ultCooldownMillis);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> ClientEvents.applySkillSync(
                dashCooldownMillis, passiveCooldownMillis, bombCharges, bombRechargeMillis, ultCooldownMillis));
        context.setPacketHandled(true);
    }
}
