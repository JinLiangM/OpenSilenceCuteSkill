package lol.silencecute.client.render.hud;

import net.minecraft.util.Mth;

public final class HudWidget {

    @FunctionalInterface
    public interface Placement {
        float[] resolve(int screenW, int screenH);
    }

    private final String id;

    private final String label;
    private float baseWidth;

    private float baseHeight;
    private final float minSize;
    private final float maxSize;
    private final Placement placement;

    private float x;
    private float y;

    private float smoothX;
    private float smoothY;
    private boolean smoothInit;
    private float width;
    private float height;
    private boolean initialized;
    private boolean visible = true;

    private float debugPadding;

    public HudWidget debugPadding(float padding) {
        this.debugPadding = Math.max(0.0F, padding);
        return this;
    }

    public float debugPadding() {
        return debugPadding;
    }

    public HudWidget(String id, String label, float baseWidth, float baseHeight,
              float minSize, float maxSize, Placement placement) {
        this.id = id;
        this.label = label;
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;
        this.minSize = minSize;
        this.maxSize = Math.max(minSize + 1.0F, maxSize);
        this.placement = placement;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public float baseWidth() {
        return baseWidth;
    }

    public float baseHeight() {
        return baseHeight;
    }

    public float minSize() {
        return minSize;
    }

    public float maxSize() {
        return maxSize;
    }

    public float scaleX() {
        return baseWidth <= 0.0F ? 1.0F : width / baseWidth;
    }

    public float scaleY() {
        return baseHeight <= 0.0F ? 1.0F : height / baseHeight;
    }

    public void setBaseHeightKeepScale(float newBaseHeight) {
        if (newBaseHeight <= 0.0F || Math.abs(newBaseHeight - this.baseHeight) < 0.01F) return;
        float scale = scaleY();
        float oldHeight = this.height;
        this.baseHeight = newBaseHeight;
        this.height = newBaseHeight * scale;
        float delta = oldHeight - this.height;
        this.y += delta;
        this.smoothY += delta;
    }

    public boolean isVisible() {
        return visible;
    }

    public HudWidget setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    int intX() {
        return Mth.floor(x());
    }

    int intY() {
        return Mth.floor(y());
    }

    int intWidth() {
        return Math.max(1, Math.round(width));
    }

    int intHeight() {
        return Math.max(1, Math.round(height));
    }

    public void ensureInitialized(int screenW, int screenH) {
        if (initialized) return;
        initialized = true;

        HudLayout.Rect saved = HudLayout.find(id);
        if (saved != null) {
            x = saved.x();
            y = saved.y();
            width = saved.w() > 0.0F ? saved.w() : baseWidth;
            height = saved.h() > 0.0F ? saved.h() : baseHeight;
        } else {
            float[] pos = defaultPosition(screenW, screenH);
            x = pos[0];
            y = pos[1];
            width = baseWidth;
            height = baseHeight;
        }
        clampToScreen(screenW, screenH);

        float bottom = y + height;
        width = baseWidth;
        height = baseHeight;
        y = bottom - height;
        smoothX = x;
        smoothY = y;

        if (!smoothInit) {
            smoothX = x;
            smoothY = y;
            smoothInit = true;
        }
        smoothX += (x - smoothX) * 0.30F;
        smoothY += (y - smoothY) * 0.30F;
    }

    void setPosition(float newX, float newY, int screenW, int screenH) {
        x = newX;
        y = newY;
        clampToScreen(screenW, screenH);
    }

    void setBounds(float newX, float newY, float newW, float newH, int screenW, int screenH) {
        x = newX;
        y = newY;
        width = newW;
        height = newH;
        clampToScreen(screenW, screenH);
    }

    void clampToScreen(int screenW, int screenH) {
        width = Mth.clamp(width, minSize, maxSize);
        height = Mth.clamp(height, minSize, maxSize);
        x = Mth.clamp(x, 0.0F, Math.max(0.0F, screenW - width));
        y = Mth.clamp(y, 0.0F, Math.max(0.0F, screenH - height));
    }

    void reset(int screenW, int screenH) {
        float[] pos = defaultPosition(screenW, screenH);
        x = pos[0];
        y = pos[1];
        width = baseWidth;
        height = baseHeight;
        clampToScreen(screenW, screenH);
        persist();
    }

    void persist() {
        HudLayout.put(id, new HudLayout.Rect(x, y, width, height));
        HudLayout.markDirty();
    }

    boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }

    private float[] defaultPosition(int screenW, int screenH) {
        if (placement == null) return new float[]{0.0F, 0.0F};
        float[] pos = placement.resolve(screenW, screenH);
        if (pos == null || pos.length < 2) return new float[]{0.0F, 0.0F};
        return pos;
    }
}
