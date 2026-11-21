package lol.silencecute.network;

import java.util.function.Supplier;

import lol.silencecute.manager.MagneticBombService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class BombThrowPacket {
    public BombThrowPacket() {
    }

    public BombThrowPacket(FriendlyByteBuf buffer) {
    }

    public void write(FriendlyByteBuf buffer) {
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                MagneticBombService.throwBomb(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
