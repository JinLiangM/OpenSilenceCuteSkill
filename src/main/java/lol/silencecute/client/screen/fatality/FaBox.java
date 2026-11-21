package lol.silencecute.client.screen.fatality;

abstract class FaBox {
    abstract float targetHeight();

    abstract void place(float targetX, float targetY, boolean snap);

    abstract float targetY();

    abstract void render(FaContext ctx, float originX, float originY, float width, float viewTop, float viewBottom,
                         float alpha);

    abstract boolean mouseClicked(FaContext ctx, double mx, double my, int button);

    abstract void mouseDragged(FaContext ctx, double mx, double my);

    abstract void mouseReleased();

    abstract boolean mouseScrolled(FaContext ctx, double mx, double my, double amount);
}
