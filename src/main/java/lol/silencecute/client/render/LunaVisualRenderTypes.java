package lol.silencecute.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LunaVisualRenderTypes extends RenderType {
    public static final RenderType ARC = create("luna_blue_arc", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 4096, false, true, CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final Map<ResourceLocation, RenderType> CHAMS_CACHE = new ConcurrentHashMap<>();

    public static RenderType getChamsType(ResourceLocation texture) {
        return CHAMS_CACHE.computeIfAbsent(texture, LunaVisualRenderTypes::createChams);
    }

    private static RenderType createChams(ResourceLocation texture) {
        return create("luna_chams_" + texture.toString().hashCode(), DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, false, true, CompositeState.builder()
                        .setShaderState(POSITION_COLOR_TEX_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(NO_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));
    }

    private LunaVisualRenderTypes() {
        super("luna_visual", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
                256, false, false, () -> { }, () -> { });
    }
}