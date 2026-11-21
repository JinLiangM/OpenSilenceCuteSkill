package lol.silencecute.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import lol.silencecute.config.ModConfigData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;III)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void silencecute$replaceWithHudChat(GuiGraphics graphics, int mouseX, int mouseY, int tickCount, CallbackInfo ci) {
        try {
            if (Minecraft.getInstance() == null) return;
            if (!ModConfigData.hudChat()) return;
            ci.cancel();
        } catch (Throwable ignored) {
        }
    }
}
