package lol.silencecute.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import lol.silencecute.Client;
import lol.silencecute.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class LunaElectricArrowModel {
    public static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            Client.MOD_ID, "item/luna_electric_arrow");
    public static final float LENGTH = 22.3F / 16.0F;
    private static ItemStack displayStack;

    private LunaElectricArrowModel() { }

    public static void render(PoseStack stack, MultiBufferSource buffers, int light) {
        Minecraft minecraft = Minecraft.getInstance();
        if (displayStack == null) displayStack = new ItemStack(ModItems.LUNA_SHOCK_ARROW.get());
        minecraft.getItemRenderer().render(displayStack, ItemDisplayContext.FIXED, false,
                stack, buffers, light, OverlayTexture.NO_OVERLAY,
                minecraft.getModelManager().getModel(MODEL));
    }

    public static void clearVisual() {
        displayStack = null;
    }
}
