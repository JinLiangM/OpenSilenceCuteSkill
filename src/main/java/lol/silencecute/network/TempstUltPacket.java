package lol.silencecute.network;

import java.util.function.Supplier;

import lol.silencecute.manager.TempstUltimateService;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class TempstUltPacket {

    public TempstUltPacket() {
    }

    public TempstUltPacket(FriendlyByteBuf buffer) {
    }

    public void write(FriendlyByteBuf buffer) {
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (TempstUltimateService.startPullBack(player)) {
                return;
            }
            TempstUltimateService.tryStart(player);
        });
        context.setPacketHandled(true);
    }
}
