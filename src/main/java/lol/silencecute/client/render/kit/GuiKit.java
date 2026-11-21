package lol.silencecute.client.render.kit;

import net.minecraft.client.gui.GuiGraphics;

public final class GuiKit {

    public static GuiGraphics g;

    private GuiKit() {
    }

    public static void bind(GuiGraphics graphics) {
        g = graphics;
    }

    public static GuiGraphics graphics() {
        return g;
    }
}
