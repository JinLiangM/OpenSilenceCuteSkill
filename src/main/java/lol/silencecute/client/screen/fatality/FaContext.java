package lol.silencecute.client.screen.fatality;

final class FaContext {
    final FaClickGui gui;

    FaDraw draw;
    float mouseX;
    float mouseY;
    float dt;

    boolean hoverEnabled = true;

    FaContext(FaClickGui gui) {
        this.gui = gui;
    }

    boolean hovered(float x, float y, float w, float h) {
        return hoverEnabled && mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
