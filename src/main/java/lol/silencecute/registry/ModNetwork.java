package lol.silencecute.registry;



import lol.silencecute.network.BombThrowPacket;
import lol.silencecute.network.DashPacket;
import lol.silencecute.network.LunaGrenadeActionPacket;
import lol.silencecute.network.LunaGrenadeSyncPacket;
import lol.silencecute.network.LunaReconArrowActionPacket;
import lol.silencecute.network.LunaReconArrowSyncPacket;
import lol.silencecute.network.LunaRevealPacket;
import lol.silencecute.network.LunaShockArrowActionPacket;
import lol.silencecute.network.LunaShockArrowSyncPacket;
import lol.silencecute.network.SkillSyncPacket;
import lol.silencecute.*;

import lol.silencecute.network.*;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.network.NetworkRegistry;

import net.minecraftforge.network.simple.SimpleChannel;



public final class ModNetwork {

    private static final String PROTOCOL = "11";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(

            ResourceLocation.fromNamespaceAndPath(Client.MOD_ID, "main"),

            () -> PROTOCOL,

            PROTOCOL::equals,

            PROTOCOL::equals);



    private static int packetId;



    private ModNetwork() {

    }



    public static void register() {

        CHANNEL.messageBuilder(DashPacket.class, packetId++)

                .encoder(DashPacket::write)

                .decoder(DashPacket::new)

                .consumerMainThread(DashPacket::handle)

                .add();

        CHANNEL.messageBuilder(AccessRequestPacket.class, packetId++)

                .encoder(AccessRequestPacket::write)

                .decoder(AccessRequestPacket::new)

                .consumerMainThread(AccessRequestPacket::handle)

                .add();

        CHANNEL.messageBuilder(SkillSyncPacket.class, packetId++)

                .encoder(SkillSyncPacket::write)

                .decoder(SkillSyncPacket::new)

                .consumerMainThread(SkillSyncPacket::handle)

                .add();

        CHANNEL.messageBuilder(BombThrowPacket.class, packetId++)

                .encoder(BombThrowPacket::write)

                .decoder(BombThrowPacket::new)

                .consumerMainThread(BombThrowPacket::handle)

                .add();

        CHANNEL.messageBuilder(LunaRevealPacket.class, packetId++)

                .encoder(LunaRevealPacket::write)

                .decoder(LunaRevealPacket::new)

                .consumerMainThread(LunaRevealPacket::handle)

                .add();

        CHANNEL.messageBuilder(LunaGrenadeActionPacket.class, packetId++)

                .encoder(LunaGrenadeActionPacket::write)

                .decoder(LunaGrenadeActionPacket::new)

                .consumerMainThread(LunaGrenadeActionPacket::handle)

                .add();

        CHANNEL.messageBuilder(LunaGrenadeSyncPacket.class, packetId++)

                .encoder(LunaGrenadeSyncPacket::write)

                .decoder(LunaGrenadeSyncPacket::new)

                .consumerMainThread(LunaGrenadeSyncPacket::handle)

                .add();

        CHANNEL.messageBuilder(LunaShockArrowActionPacket.class, packetId++)

                .encoder(LunaShockArrowActionPacket::write)

                .decoder(LunaShockArrowActionPacket::new)

                .consumerMainThread(LunaShockArrowActionPacket::handle)

                .add();

        CHANNEL.messageBuilder(LunaShockArrowSyncPacket.class, packetId++)

                .encoder(LunaShockArrowSyncPacket::write)

                .decoder(LunaShockArrowSyncPacket::new)

                .consumerMainThread(LunaShockArrowSyncPacket::handle)

                .add();

        CHANNEL.messageBuilder(LunaReconArrowActionPacket.class, packetId++)

                .encoder(LunaReconArrowActionPacket::write)

                .decoder(LunaReconArrowActionPacket::new)

                .consumerMainThread(LunaReconArrowActionPacket::handle)

                .add();

        CHANNEL.messageBuilder(LunaReconArrowSyncPacket.class, packetId++)

                .encoder(LunaReconArrowSyncPacket::write)

                .decoder(LunaReconArrowSyncPacket::new)

                .consumerMainThread(LunaReconArrowSyncPacket::handle)

                .add();

        CHANNEL.messageBuilder(TempstUltPullPacket.class, packetId++)
                .encoder(TempstUltPullPacket::write)
                .decoder(TempstUltPullPacket::new)
                .consumerMainThread(TempstUltPullPacket::handle)
                .add();
        CHANNEL.messageBuilder(TempstUltPacket.class, packetId++)
                .encoder(TempstUltPacket::write)
                .decoder(TempstUltPacket::new)
                .consumerMainThread(TempstUltPacket::handle)
                .add();
        CHANNEL.messageBuilder(TempstRollSyncPacket.class, packetId++)

                .encoder(TempstRollSyncPacket::write)

                .decoder(TempstRollSyncPacket::new)

                .consumerMainThread(TempstRollSyncPacket::handle)

                .add();

        CHANNEL.messageBuilder(AccessSyncPacket.class, packetId++)
                    .encoder(AccessSyncPacket::write)
                    .decoder(AccessSyncPacket::new)
                    .consumerMainThread(AccessSyncPacket::handle)
                    .add();
    }

}


