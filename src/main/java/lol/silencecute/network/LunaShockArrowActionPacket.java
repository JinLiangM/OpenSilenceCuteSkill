package lol.silencecute.network;

import java.util.function.Supplier;

import lol.silencecute.manager.LunaShockArrowService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class LunaShockArrowActionPacket {
    public enum Action {
        TOGGLE,
        FIRE,
        TOGGLE_BOUNCE,
        STOW
    }

    private final Action action;
    private final float power;
    private final int sequence;

    public LunaShockArrowActionPacket(Action action, float power, int sequence) {
        this.action = action;
        this.power = power;
        this.sequence = sequence;
    }

    public LunaShockArrowActionPacket(FriendlyByteBuf buffer) {
        int index = buffer.readUnsignedByte();
        Action[] values = Action.values();
        this.action = values[Math.max(0, Math.min(values.length - 1, index))];
        this.power = buffer.readFloat();
        this.sequence = buffer.readVarInt();
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeByte(action.ordinal());
        buffer.writeFloat(power);
        buffer.writeVarInt(sequence);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                LunaShockArrowService.handleAction(sender, action, power, sequence);
            }
        });
        context.setPacketHandled(true);
    }
}
