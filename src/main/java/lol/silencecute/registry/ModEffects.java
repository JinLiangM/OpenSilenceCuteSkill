package lol.silencecute.registry;

import java.util.function.Consumer;

import lol.silencecute.Client;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.Keys.MOB_EFFECTS, Client.MOD_ID);

    public static final double SPEED_BONUS = 0.60D;

    public static final RegistryObject<MobEffect> KINETIC_BOOST =
            MOB_EFFECTS.register("kinetic_boost", KineticBoostEffect::new);

    private ModEffects() {
    }

    private static final class KineticBoostEffect extends MobEffect {
        private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(
                Client.MOD_ID, "textures/gui/kinetic_boost.png");
        private static final String SPEED_MODIFIER_UUID = "5b7c9e21-3f46-4a58-90d2-71ce8a6f4b03";

        private KineticBoostEffect() {
            super(MobEffectCategory.BENEFICIAL, 0x7CAFCB);
            addAttributeModifier(Attributes.MOVEMENT_SPEED,
                    SPEED_MODIFIER_UUID,
                    SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }

        @Override
        public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
            consumer.accept(new IClientMobEffectExtensions() {
                @Override
                public boolean isVisibleInGui(MobEffectInstance instance) {
                    return false;
                }

                @Override
                public boolean isVisibleInInventory(MobEffectInstance instance) {
                    return false;
                }
            });
        }
    }
}