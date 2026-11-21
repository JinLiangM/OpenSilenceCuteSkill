package lol.silencecute.network;

import java.util.function.Supplier;

import lol.silencecute.access.AccessService;
import lol.silencecute.api.Operator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class AccessRequestPacket {

    public static final int QUERY = 0;

    public static final int SELECT_OPERATOR = 1;

    public static final int NO_COOLDOWN = 2;

    private final int action;

    private final String operator;

    private final boolean value;

    public AccessRequestPacket(int action) {
        this(action, "", false);
    }

    public AccessRequestPacket(int action, String operator, boolean value) {
        this.action = action;
        this.operator = operator == null ? "" : operator;
        this.value = value;
    }

    public AccessRequestPacket(FriendlyByteBuf buf) {
        this.action = buf.readVarInt();
        this.operator = buf.readUtf(32);
        this.value = buf.readBoolean();
    }

    public static AccessRequestPacket select(Operator operator) {
        return new AccessRequestPacket(SELECT_OPERATOR, (operator == null ? Operator.LUNA : operator).name(), false);
    }

    public static AccessRequestPacket noCooldown(boolean enabled) {
        return new AccessRequestPacket(NO_COOLDOWN, "", enabled);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.action);
        buf.writeUtf(this.operator, 32);
        buf.writeBoolean(this.value);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) return;

            switch (this.action) {
                case SELECT_OPERATOR -> {
                    Operator requested = Operator.byInput(this.operator);
                    if (requested == null || requested == Operator.NONE) {
                        AccessService.sync(sender);
                        return;
                    }
                    if (!AccessService.canChoose(sender)) {
                        AccessService.sync(sender);
                        return;
                    }
                    AccessService.setOperator(sender, requested);
                }
                case NO_COOLDOWN -> AccessService.setNoCooldown(sender, this.value);
                default -> AccessService.sync(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
