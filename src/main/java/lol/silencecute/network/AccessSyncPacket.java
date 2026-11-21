package lol.silencecute.network;

import java.util.function.Supplier;

import lol.silencecute.access.AccessService;
import lol.silencecute.api.Operator;
import lol.silencecute.client.AccessClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public final class AccessSyncPacket {

    private final String operator;

    private final boolean canChoose;

    private final boolean admin;

    private final boolean explicit;

    private final boolean noCooldown;

    public AccessSyncPacket(Operator operator, boolean canChoose, boolean admin,
                            boolean explicit, boolean noCooldown) {
        this.operator = (operator == null ? AccessService.DEFAULT_OPERATOR : operator).name();
        this.canChoose = canChoose;
        this.admin = admin;
        this.explicit = explicit;
        this.noCooldown = noCooldown;
    }

    public AccessSyncPacket(FriendlyByteBuf buf) {
        this.operator = buf.readUtf(32);
        int flags = buf.readUnsignedByte();
        this.canChoose = (flags & 1) != 0;
        this.admin = (flags & 2) != 0;
        this.explicit = (flags & 4) != 0;
        this.noCooldown = (flags & 8) != 0;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.operator, 32);
        int flags = 0;
        if (this.canChoose) flags |= 1;
        if (this.admin) flags |= 2;
        if (this.explicit) flags |= 4;
        if (this.noCooldown) flags |= 8;
        buf.writeByte(flags);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Operator parsed = Operator.byInput(this.operator);
            AccessClient.apply(parsed, this.canChoose, this.admin, this.explicit, this.noCooldown);
        });
        context.setPacketHandled(true);
    }
}
