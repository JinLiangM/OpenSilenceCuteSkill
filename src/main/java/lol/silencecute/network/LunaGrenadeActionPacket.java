package lol.silencecute.network;

import java.util.function.Supplier;

import lol.silencecute.manager.LunaGrenadeService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class LunaGrenadeActionPacket {
    public enum Action {
        EQUIP,
        PRIME,
        THROW,
        CANCEL
    }

    private final Action action;

    public LunaGrenadeActionPacket(Action action) {
        this.action = action;
    }

    public LunaGrenadeActionPacket(FriendlyByteBuf buffer) {
        int index = buffer.readUnsignedByte();
        Action[] values = Action.values();
        this.action = values[Math.max(0, Math.min(values.length - 1, index))];
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeByte(action.ordinal());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                LunaGrenadeService.handleAction(sender, action);
            }
        });
        context.setPacketHandled(true);
    }
}
