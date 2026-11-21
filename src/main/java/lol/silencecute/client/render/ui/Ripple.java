package lol.silencecute.client.render.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class Ripple {

    private static final float MAX_RADIUS = 70.0F;

    private final List<Spot> spots = new ArrayList<>();

    public void add(float x, float y) {
        if (spots.size() > 6) spots.remove(0);
        spots.add(new Spot(x, y));
    }

    public void draw(GuiGraphics graphics, float x, float y, float width, float height,
                     float originX, float originY, float scale) {
        if (spots.isEmpty()) return;

        graphics.flush();
        graphics.enableScissor(
                (int) Math.floor(originX + x * scale),
                (int) Math.floor(originY + y * scale),
                (int) Math.ceil(originX + (x + width) * scale),
                (int) Math.ceil(originY + (y + height) * scale));

        Iterator<Spot> iterator = spots.iterator();
        while (iterator.hasNext()) {
            Spot spot = iterator.next();
            spot.progress = Anim.ease(spot.progress, MAX_RADIUS, 0.18F);
            float fade = 1.0F - Mth.clamp(spot.progress / MAX_RADIUS, 0.0F, 1.0F);
            if (fade <= 0.02F) {
                iterator.remove();
                continue;
            }
            UiDraw.circle(graphics, spot.x, spot.y, spot.progress,
                    UiDraw.rgba(255, 255, 255, Math.round(fade * 130.0F)));
        }

        graphics.flush();
        graphics.disableScissor();
    }

    private static final class Spot {
        private final float x;
        private final float y;
        private float progress;

        private Spot(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
