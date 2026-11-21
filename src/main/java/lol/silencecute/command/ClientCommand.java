package lol.silencecute.command;

import lol.silencecute.Client;
import lol.silencecute.api.Operator;
import lol.silencecute.client.AccessClient;
import lol.silencecute.client.ClientEvents;
import lol.silencecute.client.lang.Lang;
import lol.silencecute.client.render.hud.notification.NotificationRea1;
import lol.silencecute.modules.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Client.MOD_ID, value = Dist.CLIENT)
public final class ClientCommand {

    private ClientCommand() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("deltaforce")
                .then(Commands.literal("gy")
                        .then(Commands.literal("luna")
                                .executes(context -> setOperator(Operator.LUNA)))
                        .then(Commands.literal("Vyron")
                                .executes(context -> setOperator(Operator.WEILONG)))
                        .then(Commands.literal("Tempst")
                                .executes(context -> setOperator(Operator.JIFENG)))
                        .then(Commands.literal("jifeng")
                                .executes(context -> setOperator(Operator.JIFENG)))
                        .then(Commands.literal("none")
                                .executes(context -> setOperator(Operator.NONE)))
                )
                .then(Commands.literal("hitbox")
                        .executes(context -> toggleHitbox()))
        );

        event.getDispatcher().register(Commands.literal("administer")
                .executes(context -> openAdminister()));
    }

    private static int openAdminister() {
        if (!AccessClient.isAdmin()) {
            NotificationRea1.error("鉴权未通过！");
            return 0;
        }
        ClientEvents.openClickGui(Category.ADMIN);
        return 1;
    }

    private static int setOperator(Operator operator) {
        if (operator == Operator.NONE) {
            chat("§c[rice] 不能选\"无干员\"：干员功能常开");
            return 0;
        }
        if (!AccessClient.requestOperator(operator)) {
            return 0;
        }
        chat("§a[rice] Done! state: §e" + Lang.operator(operator));
        return 1;
    }

    private static int toggleHitbox() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return 0;

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        boolean on = dispatcher.shouldRenderHitBoxes();
        dispatcher.setRenderHitBoxes(!on);
        chat("§a[rice] hitboxes: §e" + (!on));
        return 1;
    }

    private static void chat(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        minecraft.player.displayClientMessage(Component.literal(message), false);
    }
}
