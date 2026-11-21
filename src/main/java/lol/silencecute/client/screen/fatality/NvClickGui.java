package lol.silencecute.client.screen.fatality;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lol.silencecute.client.AccessClient;
import lol.silencecute.client.font.UiFont;
import lol.silencecute.client.lang.Lang;
import lol.silencecute.client.render.hud.notification.NotificationRea1;
import lol.silencecute.client.render.naven.NvPostFx;
import lol.silencecute.client.screen.BoolValue;
import lol.silencecute.modules.Category;
import lol.silencecute.client.screen.ColorValue;
import lol.silencecute.client.screen.LabelValue;
import lol.silencecute.client.screen.HudEditScreen;
import lol.silencecute.client.screen.KeyValue;
import lol.silencecute.client.screen.ModeValue;
import lol.silencecute.modules.Module;
import lol.silencecute.modules.ModuleManager;
import lol.silencecute.client.screen.NumberValue;
import lol.silencecute.client.screen.Value;
import lol.silencecute.config.ModConfigData;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NvClickGui extends Screen {

    private static final int ACCENT = 0xFF3662EC;

    private static final int NAVEN_DEFAULT_ACCENT = 0xFF3662EC;

    private static int accent() {
        return 0xFF000000 | ModConfigData.guiAccent();
    }
    private static final int PANEL = 0xB4000000;
    private static final int PANEL_EDGE = 0x1FFFFFFF;
    private static final int ROW_OFF = 0xFF191919;
    private static final int ROW_EDGE = 0x14FFFFFF;
    private static final int TRACK = 0x99000000;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xB0FFFFFF;
    private static final int TEXT_FAINT = 0x80FFFFFF;

    private static final float COLLAPSED_W = 100.0f;
    private static final float COLLAPSED_H = 140.0f;
    private static final float EXPANDED_W = 400.0f;
    private static final float EXPANDED_H = 250.0f;
    private static final float MIN_W = 320.0f;
    private static final float MIN_H = 220.0f;
    private static final float RADIUS = 5.0f;
    private static final float HEADER_H = 20.0f;
    private static final float CAT_TOP = 42.0f;
    private static final float CAT_H = 20.0f;
    private static final float CAT_STEP = 25.0f;
    private static final float LIST_W = 120.0f;
    private static final float MODULE_H = 25.0f;
    private static final float MODULE_STEP = 30.0f;
    private static final float VALUES_X = 140.0f;
    private static final float RESIZE_BOX = 10.0f;
    private static final float COLOR_POPUP_W = 180.0f;
    private static final float COLOR_POPUP_H = 154.0f;
    private static final float COLOR_SV_H = 118.0f;
    private static final float COLOR_HUE_W = 9.0f;
    private static final float COLOR_PAD = 8.0f;
    private static final float COLOR_GAP = 6.0f;
    private static final float COLOR_FOOT_H = 14.0f;
    private static final int[] HUES = {
            0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF
    };

    private final FaDraw draw = new NavenFaDraw();
    private final Screen parent;

    private static final String MATERIAL_ICONS = "material_symbols_rounded.ttf";

    private static final String UI_ICONS = "icon.ttf";
    private static final Map<String, FaFont> ICON_CACHE = new LinkedHashMap<>();

    private static final List<Category> CATEGORIES = buildCategories();

    private final Map<Category, FaMotion> catHover = new EnumMap<>(Category.class);
    private final Map<Module, FaMotion> moduleHover = new LinkedHashMap<>();
    private final Map<Module, FaMotion> moduleToggle = new LinkedHashMap<>();
    private final Map<Value<?>, FaMotion> valueMotion = new LinkedHashMap<>();
    private final Map<ModeValue, FaMotion> modeSlides = new LinkedHashMap<>();
    private final Map<NumberValue, FaMotion> sliderMotions = new LinkedHashMap<>();
    private final FaMotion widthMotion = new FaMotion(COLLAPSED_W);
    private final FaMotion heightMotion = new FaMotion(COLLAPSED_H);

    private final FaMotion openMotion = new FaMotion(0.0f);
    private boolean closing;
    private final FaMotion titleMotion = new FaMotion(0.0f);
    private final FaMotion valuePanelMotion = new FaMotion(0.0f);

    private final FaMotion titleHover = new FaMotion(0.0f);

    private ColorValue colorPopup;
    private float colorPopupX;
    private float colorPopupY;
    private final FaMotion colorPopupMotion = new FaMotion(0.0f);
    private boolean colorPopupClosing;
    private boolean draggingSatVal;
    private boolean draggingHue;
    private float[] colorHsb = {0.0f, 1.0f, 1.0f};

    private float cursorSatValX = Float.NaN;
    private float cursorSatValY = Float.NaN;
    private float hueDotY = Float.NaN;

    private Screen closeTarget;

    private PoseStack currentPose;
    private GuiGraphics currentGraphics;

    private int previewRgb;

    private float windowX;
    private float windowY;
    private float windowW = EXPANDED_W;
    private float windowH = EXPANDED_H;
    private boolean placed;

    private Category selected;
    private Module selectedModule;
    private List<Module> categoryModules;
    private List<Value<?>> renderValues;

    private static Category pendingCategory;

    public static void requestCategory(Category target) {
        pendingCategory = target;
    }

    private Category hoveringCategory;

    private boolean hoveringEditHud;
    private final FaMotion editHudHover = new FaMotion(0.0f);
    private Module hoveringModule;
    private boolean hoveringBack;
    private BoolValue hoveringBool;
    private NumberValue hoveringNumber;
    private ModeValue hoveringMode;
    private int hoveringModeIndex;
    private ColorValue hoveringColor;
    private float hoveringColorX;
    private float hoveringColorY;

    private KeyValue hoveringKey;

    private KeyValue listeningKey;

    private boolean draggingWindow;
    private boolean resizingWindow;
    private NumberValue draggingSlider;

    private float pendingSliderValue;
    private boolean sliderDirty;
    private float dragOffsetX;
    private float dragOffsetY;
    private float catScrollTarget;
    private float valueScrollTarget;
    private float catScroll;
    private float valueScroll;
    private float catContentH;
    private float valueContentH;

    private long lastNanos;

    public NvClickGui(Screen parent) {
        super(Component.literal("ClickGui"));
        this.parent = parent;
        widthMotion.snap(COLLAPSED_W);
        heightMotion.snap(collapsedHeight());
    }

    @Override
    protected void init() {
        super.init();
        AccessClient.query();
        if (!placed) {
            windowX = (width - COLLAPSED_W) * 0.5f;
            windowY = (height - collapsedHeight()) * 0.5f;
            placed = true;
        }
        clampWindow();

        Category pending = pendingCategory;
        pendingCategory = null;
        if (pending != null) {
            openCategory(pending);
        }
    }

    @Override
    public void onClose() {
        GuiTransition.reset();
        if (minecraft != null) {
            minecraft.setScreen(closeTarget != null ? closeTarget : parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        NvPostFx.beginFrame();
        long now = System.nanoTime();
        float rawDt = lastNanos == 0L ? 1.0f / 60.0f : (now - lastNanos) / 1.0e9f;
        lastNanos = now;

        float dt = rawDt > 0.15f ? 0.0f : Math.min(0.1f, rawDt);

        hoveringCategory = null;
        hoveringEditHud = false;
        hoveringModule = null;
        hoveringBack = false;
        hoveringBool = null;
        hoveringNumber = null;
        hoveringMode = null;
        hoveringColor = null;
        hoveringKey = null;

        currentPose = graphics.pose();
        currentGraphics = graphics;

        if (closing && openMotion.value() <= 0.02f) {
            onClose();
            return;
        }

        if (GuiTransition.isSwitching()) openMotion.snap(1.0f);
        float open = openMotion.toward(closing ? 0.0f : 1.0f, dt, 14.0f);

        draw.begin(graphics);
        try {
            draw.setLowQuality(draggingWindow || resizingWindow);
            try (FaDraw.Scope scope = draw.push()) {
                if (open < 0.999f) draw.opacity(open);
                drawWithTransition(draw, mouseX, mouseY, dt, open);
            }
        } finally {
            draw.closeAllClips();
        }
        GuiTransition.tick();
    }

    private void drawWithTransition(FaDraw c, float mx, float my, float dt, float open) {

        float shrink = Math.min(GuiTransition.scale(), open);
        if (shrink >= 0.999f) {
            frame(c, mx, my, dt);
            return;
        }
        float w = widthMotion.value();
        float h = heightMotion.value();
        float cw = Math.max(0.0f, w * shrink);
        float ch = Math.max(0.0f, h * shrink);
        float cx = windowX + (w - cw) * 0.5f;
        float cy = windowY + (h - ch) * 0.5f;
        try (FaDraw.Scope clip = c.clip(cx, cy, cw, ch)) {
            frame(c, mx, my, dt);
        }
    }

    private void frame(FaDraw c, float mouseX, float mouseY, float dt) {
        float targetW = selected == null ? COLLAPSED_W : windowW;
        float targetH = selected == null ? collapsedHeight() : windowH;
        float w = widthMotion.toward(targetW, dt, 16.0f);
        float h = heightMotion.toward(targetH, dt, 16.0f);

        titleMotion.toward(selected == null ? 0.0f : 1.0f, dt, 16.0f);
        valuePanelMotion.toward(renderValues != null && !renderValues.isEmpty() ? 1.0f : 0.0f, dt, 16.0f);
        clampWindow();

        float bgAlpha = ModConfigData.navenBgAlpha();
        if (draggingSlider != null && "背景不透明度".equals(draggingSlider.name)) {
            bgAlpha = pendingSliderValue;
        }
        int bg = FaTheme.alpha(0xFF000000 | (ModConfigData.navenBgColor() & 0xFFFFFF), bgAlpha);

        if (!closing && ModConfigData.rendererEnabled() && ModConfigData.navenBlur() && NvPostFx.available()) {
            blurFloatFor(windowX, windowY, w, h, RADIUS);
        }
        c.rect(windowX, windowY, w, h, RADIUS, bg);

        if (selected == null) {
            drawCollapsed(c, mouseX, mouseY, dt, w, h);
        } else {
            drawTitle(c, mouseX, mouseY, dt, w);
            drawModules(c, mouseX, mouseY, dt, w, h);
            drawValues(c, mouseX, mouseY, dt, w, h);
            drawResizeHint(c, w, h);
            drawColorPopup(c, dt);
        }
    }

    private void drawCollapsed(FaDraw c, float mouseX, float mouseY, float dt, float w, float h) {
        FaFont logo = FaTheme.logo();
        String name = "SILENCE";
        c.textMid(name, windowX + w * 0.5f - logo.width(name) * 0.5f, windowY + 15f, logo, TEXT);

        for (int i = 0; i < CATEGORIES.size(); i++) {
            Category category = CATEGORIES.get(i);
            FaMotion hover = catHover.computeIfAbsent(category, ignored -> new FaMotion(0.0f));
            float top = windowY + CAT_TOP + i * CAT_STEP;
            boolean over = mouseX >= windowX && mouseX <= windowX + w
                    && mouseY >= top && mouseY <= top + CAT_H;
            float shift = hover.toward(over ? 5.0f : 0.0f, dt, 18.0f);
            if (over) hoveringCategory = category;

            int color = over ? TEXT : TEXT_DIM;
            FaFont icon = iconFont(MATERIAL_ICONS, 14.0f);
            c.textMid(navenIcon(category), windowX + 10.0f + shift, top + CAT_H * 0.5f, icon, color);

            c.textMid(displayName(category), windowX + 27.0f + shift, top + CAT_H * 0.5f - 0.25f, FaTheme.tab(), color);
        }

        float editTop = windowY + CAT_TOP + CATEGORIES.size() * CAT_STEP;
        boolean editOver = mouseX >= windowX && mouseX <= windowX + w
                && mouseY >= editTop && mouseY <= editTop + CAT_H;
        float editShift = editHudHover.toward(editOver ? 5.0f : 0.0f, dt, 18.0f);
        hoveringEditHud = editOver;
        int editColor = editOver ? TEXT : TEXT_DIM;
        FaFont editIcon = iconFont(MATERIAL_ICONS, 14.0f);

        c.textMid("\uE3C9", windowX + 10.0f + editShift, editTop + CAT_H * 0.5f, editIcon, editColor);
        String editLabel = Lang.tr("gui.edit_hud");
        c.textMid(editLabel, windowX + 27.0f + editShift, editTop + CAT_H * 0.5f - 0.25f, FaTheme.tab(), editColor);
    }

    private static float collapsedHeight() {
        return Math.max(COLLAPSED_H, CAT_TOP + CATEGORIES.size() * CAT_STEP + CAT_H + 8.0f);
    }

    private void drawTitle(FaDraw c, float mouseX, float mouseY, float dt, float w) {
        String title = "< " + displayName(selected);
        if (selectedModule != null) {
            title += " / " + displayName(selectedModule);
        }
        FaFont font = FaTheme.tab();
        float alpha = titleMotion.value();
        int color = FaTheme.alpha(TEXT, Math.max(0.35f, alpha));

        hoveringBack = mouseX >= windowX + 5.0f && mouseX <= windowX + 10.0f + font.width(title)
                && mouseY >= windowY + 2.0f && mouseY <= windowY + HEADER_H;

        float x = windowX + 7.0f - 2.0f * titleHover.toward(hoveringBack ? 1.0f : 0.0f, dt, 20.0f);
        c.textMid(title, x, windowY + HEADER_H * 0.5f, font, color);
    }

    private void drawModules(FaDraw c, float mouseX, float mouseY, float dt, float w, float h) {
        if (categoryModules == null) categoryModules = modulesOf(selected);

        float top = windowY + HEADER_H;
        float viewH = Math.max(1.0f, h - HEADER_H - 5.0f);
        catContentH = categoryModules.size() * MODULE_STEP + 5.0f;
        float maxScroll = Math.min(0.0f, viewH - catContentH);
        catScrollTarget = clamp(catScrollTarget, maxScroll, 0.0f);
        catScroll = approach(catScroll, catScrollTarget, dt, 18.0f);

        boolean inList = mouseX >= windowX && mouseX <= windowX + LIST_W + 8.0f
                && mouseY >= top && mouseY <= top + viewH;

        try (FaDraw.Scope clip = c.clip(windowX, top, LIST_W + 8.0f, viewH)) {
            float offset = 0.0f;
            for (Module module : categoryModules) {
                float rowY = top + offset + catScroll;
                FaMotion hover = moduleHover.computeIfAbsent(module, ignored -> new FaMotion(0.0f));
                FaMotion on = moduleToggle.computeIfAbsent(module, ignored -> new FaMotion(module.state ? 1.0f : 0.0f));

                boolean over = inList && mouseX >= windowX + 5.0f && mouseX <= windowX + 5.0f + LIST_W
                        && mouseY >= rowY && mouseY <= rowY + MODULE_H;
                hover.toward(over ? 1.0f : 0.0f, dt, 18.0f);
                float enabled = on.toward(module.state ? 1.0f : 0.0f, dt, 16.0f);
                if (over) hoveringModule = module;

                int base = FaTheme.mix(ROW_OFF, accent(), enabled);
                c.rect(windowX + 5.0f, rowY, LIST_W, MODULE_H, RADIUS, base);
                if (hover.value() > 0.01f) {
                    c.rect(windowX + 5.0f, rowY, LIST_W, MODULE_H, RADIUS,
                            FaTheme.alpha(0xFFFFFFFF, 0.18f * hover.value()));
                }

                c.textMid(displayName(module), windowX + 13.0f, rowY + MODULE_H * 0.5f, FaTheme.tab(), TEXT);
                offset += MODULE_STEP;
            }
        }

        if (catContentH > viewH) {
            float ratio = viewH / catContentH;
            float barH = Math.max(20.0f, viewH * ratio);
            float progress = maxScroll >= 0.0f ? 0.0f : (catScroll / maxScroll);
            float barY = top + progress * (viewH - barH);
            c.rect(windowX + LIST_W + 7.0f, barY, 3.0f, barH, 1.5f, 0x553662EC);
        }
    }

    private void drawValues(FaDraw c, float mouseX, float mouseY, float dt, float w, float h) {
        if (renderValues == null || renderValues.isEmpty()) return;

        float left = windowX + VALUES_X;
        float right = windowX + w - 12.0f;
        float top = windowY + HEADER_H;
        float viewH = Math.max(1.0f, h - HEADER_H - 5.0f);
        float fade = valuePanelMotion.value();

        boolean inPanel = mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= top + viewH;

        valueScrollTarget = clamp(valueScrollTarget, Math.min(0.0f, viewH - valueContentH), 0.0f);
        valueScroll = approach(valueScroll, valueScrollTarget, dt, 18.0f);

        try (FaDraw.Scope clip = c.clip(left - 6.0f, top, right - left + 12.0f, viewH)) {
            float y = top + 8.0f + valueScroll;
            for (Value<?> value : renderValues) {
                if (!value.isAvailable() || value.isHidden()) continue;
                int color = FaTheme.alpha(TEXT, fade);

                    if (value instanceof LabelValue labelValue) {

                        c.textMid(Lang.byZh(labelValue.name), left, y + 5.0f, FaTheme.small(),
                                FaTheme.alpha(TEXT_FAINT, fade));
                        c.rect(left, y + 14.0f, Math.max(0.0f, right - left), 1.0f, 0.0f,
                                FaTheme.alpha(0x24FFFFFF, fade));
                        y += 22.0f;
                        continue;
                    }

                if (value instanceof BoolValue bool) {
                    FaMotion motion = valueMotion.computeIfAbsent(bool, ignored -> new FaMotion(bool.get() ? 1.0f : 0.0f));
                    float on = motion.toward(bool.get() ? 1.0f : 0.0f, dt, 16.0f);
                    boolean over = inPanel && mouseX >= left && mouseX <= right
                            && mouseY >= y && mouseY <= y + 14.0f;
                    if (over) hoveringBool = bool;

                    c.rect(left, y + 5.0f, 12.0f, 12.0f, 2.0f, FaTheme.alpha(0xFF000000, 0.6f));
                    c.rect(left + 2.0f, y + 7.0f, 8.0f, 8.0f, 2.0f, FaTheme.mix(accent() & 0x00FFFFFF, accent(), on));

                    c.textMid(displayName(value), left + 16.0f, y + 10.0f, FaTheme.tab(), color);
                    y += 20.0f;
                } else if (value instanceof NumberValue number) {
                    String label = displayName(value);
                    c.textMid(label, left, y + 5.0f, FaTheme.tab(), color);

                    boolean editing = draggingSlider == number;
                    float shownValue = editing ? pendingSliderValue : number.get();
                    String shown = formatValue(shownValue);
                    c.textMid(shown, right - FaTheme.small().width(shown), y + 5.0f, FaTheme.small(), TEXT_DIM);

                    float trackY = y + 16.0f;
                    c.rect(left, trackY, right - left, 5.0f, 3.0f, TRACK);
                    float targetRatio = clamp((shownValue - number.min) / Math.max(0.0001f, number.max - number.min),
                            0.0f, 1.0f);

                    FaMotion sliderMotion = sliderMotions.get(number);
                    if (sliderMotion == null) {
                        sliderMotion = new FaMotion(targetRatio);
                        sliderMotions.put(number, sliderMotion);
                    }
                    float ratio = sliderMotion.toward(targetRatio, dt, 22.0f);
                    float fill = (right - left) * ratio;
                    c.rect(left, trackY, Math.max(0.0f, fill), 5.0f, 3.0f, accent());
                    c.rect(left + fill - 5.0f, trackY - 2.5f, 10.0f, 10.0f, 5.0f, 0xFFFFFFFF);

                    if (inPanel && mouseX >= left && mouseX <= right
                            && mouseY >= trackY - 6.0f && mouseY <= trackY + 8.0f) {
                        hoveringNumber = number;
                    }
                    y += 30.0f;
                } else if (value instanceof ModeValue mode) {
                    c.textMid(displayName(value), left, y + 4.0f, FaTheme.tab(), color);
                    y += 15.0f;
                    float x = left;
                    List<float[]> slots = new ArrayList<>();
                    int selectedIndex = -1;
                    for (int i = 0; i < mode.modes.length; i++) {
                        String label = Lang.option(mode.modes[i]);
                        float chipW = FaTheme.small().width(label) + 21.0f;
                        if (x + chipW > right) {
                            x = left;
                            y += 18.0f;
                        }
                        boolean on = mode.is(mode.modes[i]);
                        if (on) selectedIndex = i;
                        boolean over = inPanel && mouseX >= x && mouseX <= x + chipW
                                && mouseY >= y && mouseY <= y + 13.0f;
                        if (over) {
                            hoveringMode = mode;
                            hoveringModeIndex = i;
                        }
                        slots.add(new float[]{x, y});
                        c.rect(x, y + 3.0f, 10.0f, 10.0f, 5.0f, FaTheme.alpha(0xFF000000, 0.6f));

                        c.textMid(label, x + 13.0f, y + 8.0f, FaTheme.small(), on ? TEXT : TEXT_DIM);
                        x += chipW;
                    }

                    if (selectedIndex >= 0 && !slots.isEmpty()) {
                        FaMotion slide = modeSlides.get(mode);
                        if (slide == null) {
                            slide = new FaMotion(selectedIndex);
                            modeSlides.put(mode, slide);
                        }
                        float pos = slide.toward(selectedIndex, dt, 20.0f);
                        int a = Math.max(0, Math.min(slots.size() - 1, (int) Math.floor(pos)));
                        int b = Math.min(slots.size() - 1, a + 1);
                        float f = clamp(pos - a, 0.0f, 1.0f);
                        float sx = slots.get(a)[0] + (slots.get(b)[0] - slots.get(a)[0]) * f;
                        float sy = slots.get(a)[1] + (slots.get(b)[1] - slots.get(a)[1]) * f;

                        c.rect(sx + 1.5f, sy + 4.5f, 7.0f, 7.0f, 3.5f, accent());
                    }
                    y += 22.0f;
                } else if (value instanceof KeyValue keyValue) {
                    String label = displayName(value);
                    c.textMid(label, left, y + 5.0f, FaTheme.tab(), color);

                    boolean binding = listeningKey == keyValue;
                    String text = binding ? "..." : FaKeyRow.keyName(keyValue.get());
                    float fw = Math.max(46.0f, FaTheme.small().width(text) + 18.0f);
                    float fx = right - fw;
                    boolean hot = binding || (inPanel && mouseX >= fx && mouseX <= fx + fw
                            && mouseY >= y && mouseY <= y + 14.0f);
                    if (hot) hoveringKey = keyValue;

                    c.rect(fx, y, fw, 14.0f, 3.0f, FaTheme.alpha(0xFF000000, 0.55f));
                    if (binding) {
                        c.outline(fx, y, fw, 14.0f, 3.0f, 1.0f, accent());
                    }
                    int keyColor = binding ? accent() : (hot ? TEXT : TEXT_DIM);
                    c.textMid(text, fx + (fw - FaTheme.small().width(text)) * 0.5f, y + 7.0f,
                            FaTheme.small(), keyColor);
                    y += 20.0f;
                } else if (value instanceof ColorValue colorValue) {
                    String label = displayName(value);
                    c.textMid(label, left, y + 5.0f, FaTheme.tab(), color);
                    float dot = 11.0f;
                    float dotX = right - dot;
                    float dotY = y + 0.5f;

                    int live = colorPopup == colorValue ? previewRgb : (colorValue.get() & 0xFFFFFF);
                    int rgb = 0xFF000000 | live;
                    c.rect(dotX, dotY, dot, dot, dot * 0.5f, rgb);
                    if (inPanel && mouseX >= dotX && mouseX <= dotX + dot
                            && mouseY >= dotY && mouseY <= dotY + dot) {
                        hoveringColor = colorValue;
                        hoveringColorX = dotX + dot;
                        hoveringColorY = dotY + dot * 0.5f;
                    }
                    y += 20.0f;
                } else {
                    String label = displayName(value);
                    c.textMid(label, left, y + 5.0f, FaTheme.tab(), color);
                    String shown = value.text();
                    if (shown != null && !shown.isEmpty()) {
                        c.textMid(shown, right - FaTheme.small().width(shown), y + 5.0f, FaTheme.small(), TEXT_DIM);
                    }
                    y += 20.0f;
                }
            }
            valueContentH = y - top - valueScroll + 6.0f;
        }

        if (valueContentH > viewH) {
            float ratio = viewH / valueContentH;
            float barH = Math.max(20.0f, viewH * ratio);
            float maxScroll = Math.min(0.0f, viewH - valueContentH);
            float progress = maxScroll >= 0.0f ? 0.0f : (valueScroll / maxScroll);
            float barY = top + progress * (viewH - barH);
            c.rect(right + 4.0f, barY, 3.0f, barH, 1.5f, 0x553662EC);
        }
    }

    private void drawResizeHint(FaDraw c, float w, float h) {
        FaFont resize = iconFont(UI_ICONS, 13.0f);
        c.textRightMid("\uE904", windowX + w - 3.0f, windowY + h - 8.0f, resize, TEXT_FAINT);
    }

    private void openColorPopup(ColorValue value, float anchorX, float anchorY) {
        colorPopup = value;
        colorPopupClosing = false;
        colorPopupMotion.snap(0.0f);
        previewRgb = value.get() & 0xFFFFFF;
        colorHsb = java.awt.Color.RGBtoHSB((previewRgb >> 16) & 0xFF,
                (previewRgb >> 8) & 0xFF, previewRgb & 0xFF, null);
        applyColorPreview();
        colorPopupX = anchorX + 6.0f;
        if (colorPopupX + COLOR_POPUP_W > width - 4.0f) colorPopupX = anchorX - COLOR_POPUP_W - 6.0f;
        colorPopupX = clamp(colorPopupX, 4.0f, Math.max(4.0f, width - COLOR_POPUP_W - 4.0f));
        colorPopupY = clamp(anchorY - COLOR_POPUP_H * 0.5f, 4.0f,
                Math.max(4.0f, height - COLOR_POPUP_H - 4.0f));
    }

    private void closeColorPopup() {
        if (colorPopup == null) return;
        commitColor();
        colorPopupClosing = true;
        draggingSatVal = false;
        draggingHue = false;
    }

    private void commitColor() {
        if (colorPopup == null) return;
        ModConfigData.clearAccentPreview();
        ModConfigData.clearBgColorPreview();
        try {
            colorPopup.set(previewRgb & 0xFFFFFF);
        } catch (Exception ignored) {

        }
    }

    private float svX() { return colorPopupX + COLOR_PAD; }
    private float svY() { return colorPopupY + COLOR_PAD; }
    private float svWidth() { return COLOR_POPUP_W - COLOR_PAD * 2.0f - COLOR_HUE_W - COLOR_GAP; }
    private float hueX() { return svX() + svWidth() + COLOR_GAP; }

    private void drawColorPopup(FaDraw c, float dt) {
        if (colorPopup == null) return;

        float p = colorPopupMotion.toward(colorPopupClosing ? 0.0f : 1.0f, dt, 18.0f);
        if (colorPopupClosing && p <= 0.02f) {
            colorPopup = null;
            colorPopupClosing = false;
            return;
        }

        float w = COLOR_POPUP_W * p;
        float h = COLOR_POPUP_H * p;
        float cx = colorPopupX + (COLOR_POPUP_W - w) * 0.5f;
        float cy = colorPopupY + (COLOR_POPUP_H - h) * 0.5f;

        blurFloatFor(cx, cy, w, h, 5.0f);
        try (FaDraw.Scope clip = c.clip(cx, cy, Math.max(0.0f, w), Math.max(0.0f, h))) {
            c.rect(colorPopupX, colorPopupY, COLOR_POPUP_W, COLOR_POPUP_H, 5.0f, popupBg());

            float x = svX();
            float y = svY();
            float sw = svWidth();
            float sh = COLOR_SV_H;

            int pure = 0xFF000000 | java.awt.Color.HSBtoRGB(colorHsb[0], 1.0f, 1.0f);
            c.rect(x, y, sw, sh, 2.0f, pure);
            c.horizontal(x, y, sw, sh, 0xFFFFFFFF, 0x00FFFFFF);
            c.vertical(x, y, sw, sh, 0x00000000, 0xFF000000);

            float hx = hueX();
            float seg = sh / HUES.length;
            for (int i = 0; i < HUES.length; i++) {
                c.vertical(hx, y + seg * i, COLOR_HUE_W, seg + 0.5f,
                        HUES[i], HUES[(i + 1) % HUES.length]);
            }

            float targetPx = x + colorHsb[1] * sw;
            float targetPy = y + (1.0f - colorHsb[2]) * sh;
            if (Float.isNaN(cursorSatValX)) {
                cursorSatValX = targetPx;
                cursorSatValY = targetPy;
            }
            cursorSatValX = approach(cursorSatValX, targetPx, dt, 30.0f);
            cursorSatValY = approach(cursorSatValY, targetPy, dt, 30.0f);

            float cursorX = cursorSatValX - 4.0f;
            float cursorY = cursorSatValY - 4.0f;
            c.rect(cursorX, cursorY, 8.0f, 8.0f, 4.0f, 0xFF000000 | (previewRgb & 0xFFFFFF));
            c.outline(cursorX, cursorY, 8.0f, 8.0f, 4.0f, 1.5f, 0xFFFFFFFF);

            float targetHy = y + colorHsb[0] * sh;
            if (Float.isNaN(hueDotY)) hueDotY = targetHy;
            hueDotY = approach(hueDotY, targetHy, dt, 30.0f);
            c.rect(hx - 2.0f, hueDotY - 1.5f, COLOR_HUE_W + 4.0f, 3.0f, 1.5f, 0xFFFFFFFF);

            String hex = String.format("#%06X", previewRgb & 0xFFFFFF);
            c.textMid(hex, x, y + sh + 8.0f + COLOR_FOOT_H * 0.5f, FaTheme.small(), 0xB0FFFFFF);
        }
    }

    private boolean handleColorPopupClick(float mx, float my) {
        if (colorPopup == null) return false;
        float x = svX();
        float y = svY();
        float sw = svWidth();
        if (mx >= x && mx <= x + sw && my >= y && my <= y + COLOR_SV_H) {
            draggingSatVal = true;
            updateSv(mx, my);
            return true;
        }
        float hx = hueX();
        if (mx >= hx - 2.0f && mx <= hx + COLOR_HUE_W + 2.0f && my >= y && my <= y + COLOR_SV_H) {
            draggingHue = true;
            updateHue(my);
            return true;
        }
        return mx >= colorPopupX && mx <= colorPopupX + COLOR_POPUP_W
                && my >= colorPopupY && my <= colorPopupY + COLOR_POPUP_H;
    }

    private void updateSv(float mx, float my) {
        if (colorPopup == null) return;
        float sw = Math.max(1.0f, svWidth());
        colorHsb[1] = clamp((mx - svX()) / sw, 0.0f, 1.0f);
        colorHsb[2] = 1.0f - clamp((my - svY()) / COLOR_SV_H, 0.0f, 1.0f);
        applyHsb();
    }

    private void updateHue(float my) {
        if (colorPopup == null) return;
        colorHsb[0] = clamp((my - svY()) / COLOR_SV_H, 0.0f, 0.9999f);
        applyHsb();
    }

    private void applyHsb() {
        previewRgb = java.awt.Color.HSBtoRGB(colorHsb[0], colorHsb[1], colorHsb[2]) & 0xFFFFFF;
        applyColorPreview();
    }

    private void applyColorPreview() {
        if (colorPopup == null) return;
        String name = colorPopup.name;
        if ("GUI颜色".equals(name)) {
            ModConfigData.setAccentPreview(previewRgb);
        } else if ("背景颜色".equals(name)) {
            ModConfigData.setBgColorPreview(previewRgb);
        }
    }

    private static FaFont iconFont(String file, float size) {
        return ICON_CACHE.computeIfAbsent(file + "@" + size,
                ignored -> new FaFont.Game(new UiFont(file, size)));
    }

    private static String navenIcon(Category category) {
        return switch (category) {
            case RENDER -> "\uE40A";
            case MISC -> "\uE5D3";
            case ADMIN -> "\uE5D3";
        };
    }

    @Override
    public boolean mouseClicked(double px, double py, int button) {

        if (closing || GuiTransition.isSwitching()) return true;
        float mx = (float) px;
        float my = (float) py;
        float w = widthMotion.value();
        float h = heightMotion.value();

        if (button == 0 && colorPopup != null) {
            if (handleColorPopupClick(mx, my)) return true;
            closeColorPopup();
            return true;
        }

        if (selected != null && button == 0
                && mx >= windowX + w - RESIZE_BOX && mx <= windowX + w
                && my >= windowY + h - RESIZE_BOX && my <= windowY + h) {
            resizingWindow = true;
            dragOffsetX = mx;
            dragOffsetY = my;
            return true;
        }

        if (selected == null) {
            if (button == 0 && hoveringEditHud) {

                beginClose(new HudEditScreen());
                return true;
            }
            if (button == 0 && hoveringCategory != null) {
                openCategory(hoveringCategory);
                return true;
            }

            if (button == 0 && inWindow(mx, my, w, h)) {
                draggingWindow = true;
                dragOffsetX = mx - windowX;
                dragOffsetY = my - windowY;
                return true;
            }
            return super.mouseClicked(px, py, button);
        }

        if (button == 0 && mx >= windowX && mx <= windowX + w
                && my >= windowY && my <= windowY + HEADER_H) {
            if (hoveringBack) {
                closeColorPopup();
                selected = null;
                selectedModule = null;
                renderValues = null;
                categoryModules = null;
                catScroll = catScrollTarget = 0.0f;
                valueScroll = valueScrollTarget = 0.0f;
                return true;
            }
            draggingWindow = true;
            dragOffsetX = mx - windowX;
            dragOffsetY = my - windowY;
            return true;
        }

        if (hoveringModule != null && (button == 0 || button == 1)) {
            if (hoveringModule instanceof lol.silencecute.modules.impl.misc.OperatorModule
                    && AccessClient.isLocked()) {
                NotificationRea1.error("鉴权未通过！");
                return true;
            }

            closeColorPopup();

            if (button == 1 || !hoveringModule.toggleable) {
                selectedModule = hoveringModule;
                renderValues = valuesOf(hoveringModule);
                valueScroll = valueScrollTarget = 0.0f;
            } else {
                hoveringModule.toggle();
            }
            return true;
        }

        if (listeningKey != null) {
            listeningKey.set(button == 1 ? 0 : KeyValue.mouseKey(button));
            listeningKey = null;
            return true;
        }

        if (button == 0 && hoveringKey != null) {
            listeningKey = listeningKey == hoveringKey ? null : hoveringKey;
            return true;
        }
        if (button == 1 && hoveringKey != null) {
            hoveringKey.set(0);
            listeningKey = null;
            return true;
        }
        if (button == 0 && hoveringColor != null) {
            openColorPopup(hoveringColor, hoveringColorX, hoveringColorY);
            return true;
        }
        if (button == 0 && hoveringBool != null) {
            hoveringBool.set(!hoveringBool.get());
            return true;
        }
        if (button == 0 && hoveringNumber != null) {
            draggingSlider = hoveringNumber;
            dragSliderTo(mx, w);
            return true;
        }
        if (button == 0 && hoveringMode != null) {
            hoveringMode.set(hoveringMode.modes[hoveringModeIndex]);
            return true;
        }

        return super.mouseClicked(px, py, button);
    }

    @Override
    public boolean mouseReleased(double px, double py, int button) {
        draggingWindow = false;
        resizingWindow = false;

        if (draggingSlider != null) {
            if (sliderDirty) draggingSlider.set(pendingSliderValue);
            draggingSlider = null;
            sliderDirty = false;
        }

        if (draggingSatVal || draggingHue) commitColor();
        draggingSatVal = false;
        draggingHue = false;
        return super.mouseReleased(px, py, button);
    }

    @Override
    public boolean mouseDragged(double px, double py, int button, double dx, double dy) {
        float mx = (float) px;
        float my = (float) py;
        if (draggingSatVal) {
            updateSv(mx, my);
            return true;
        }
        if (draggingHue) {
            updateHue(my);
            return true;
        }
        if (draggingWindow) {
            windowX = mx - dragOffsetX;
            windowY = my - dragOffsetY;
            clampWindow();
            return true;
        }
        if (resizingWindow) {
            windowW = Math.max(MIN_W, windowW + (mx - dragOffsetX));
            windowH = Math.max(MIN_H, windowH + (my - dragOffsetY));
            dragOffsetX = mx;
            dragOffsetY = my;
            clampWindow();
            return true;
        }
        if (draggingSlider != null) {
            dragSliderTo(mx, widthMotion.value());
            return true;
        }
        return super.mouseDragged(px, py, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double px, double py, double delta) {
        float h = heightMotion.value();
        float viewH = Math.max(1.0f, h - HEADER_H - 5.0f);
        if (selected != null && px >= windowX + VALUES_X - 4.0f && px <= windowX + widthMotion.value()) {
            valueScrollTarget = clamp(valueScrollTarget + (float) delta * 20.0f,
                    Math.min(0.0f, viewH - valueContentH), 0.0f);
            return true;
        }
        if (selected != null) {
            catScrollTarget = clamp(catScrollTarget + (float) delta * 20.0f,
                    Math.min(0.0f, viewH - catContentH), 0.0f);
            return true;
        }
        return super.mouseScrolled(px, py, delta);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {

        if (listeningKey != null) {
            boolean clear = key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
                    || key == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE
                    || key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE;
            listeningKey.set(clear ? 0 : key);
            listeningKey = null;
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {

            if (colorPopup != null) {
                closeColorPopup();
                return true;
            }

            if (selectedModule != null) {
                selectedModule = null;
                renderValues = null;
                return true;
            }
            if (selected != null) {
                selected = null;
                categoryModules = null;
                return true;
            }
            beginClose();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private void beginClose() {
        beginClose(null);
    }

    private void beginClose(Screen target) {
        if (closing) return;
        closing = true;
        closeTarget = target;
        draggingWindow = false;
        resizingWindow = false;
        draggingSlider = null;
    }

    private void openCategory(Category category) {
        if (category == null) return;
        if (category == Category.ADMIN && !AccessClient.isAdmin()) {
            NotificationRea1.error("鉴权未通过！");
            return;
        }
        selected = category;
        selectedModule = null;
        renderValues = null;
        categoryModules = modulesOf(category);
        catScroll = catScrollTarget = 0.0f;
        valueScroll = valueScrollTarget = 0.0f;

        if (windowX + windowW > width - 4.0f) windowX = Math.max(4.0f, width - windowW - 4.0f);
        if (windowY + windowH > height - 4.0f) windowY = Math.max(4.0f, height - windowH - 4.0f);
    }

    private void dragSliderTo(float mouseX, float w) {
        if (draggingSlider == null) return;
        float left = windowX + VALUES_X;
        float right = windowX + w - 12.0f;
        float span = Math.max(1.0f, right - left);
        float stage = (mouseX - left) / span;
        float value = draggingSlider.min + (draggingSlider.max - draggingSlider.min) * stage;
        if (draggingSlider.inc > 0.0f) {
            value = Math.round(value / draggingSlider.inc) * draggingSlider.inc;
        }
        pendingSliderValue = clamp(value, draggingSlider.min, draggingSlider.max);
        sliderDirty = true;
    }

    private static String formatValue(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001f) return String.valueOf(Math.round(value));
        String text = String.format(java.util.Locale.ROOT, "%.2f", value);
        while (text.endsWith("0")) text = text.substring(0, text.length() - 1);
        if (text.endsWith(".")) text = text.substring(0, text.length() - 1);
        return text;
    }

    private float liveBlurStrength() {
        if (draggingSlider != null && draggingSlider.name != null
                && "模糊力度".equals(draggingSlider.name)) {
            return pendingSliderValue;
        }
        return ModConfigData.navenBlurStrength();
    }

    private static boolean isThemeMode(ModeValue mode) {
        if (mode.modes.length == 0) return false;
        for (String option : mode.modes) {
            String lower = option.toLowerCase(java.util.Locale.ROOT);
            if (!lower.equals("暗") && !lower.equals("亮")
                    && !lower.equals("dark") && !lower.equals("light")) {
                return false;
            }
        }
        return true;
    }

    private void clampWindow() {
        float w = widthMotion.value();
        float h = heightMotion.value();

        windowX = clamp(windowX, 0.0f, Math.max(0.0f, width - w));
        windowY = clamp(windowY, 0.0f, Math.max(0.0f, height - h));
        windowW = clamp(windowW, MIN_W, Math.max(MIN_W, width - 8.0f));
        windowH = clamp(windowH, MIN_H, Math.max(MIN_H, height - 8.0f));
    }

    private boolean inWindow(float mx, float my, float w, float h) {
        return mx >= windowX && mx <= windowX + w && my >= windowY && my <= windowY + h;
    }

    private static final float POPUP_EXTRA_ALPHA = 0.10f;

    private static int popupBg() {
        float alpha = Math.min(0.85f, ModConfigData.navenBgAlpha() + POPUP_EXTRA_ALPHA);
        return FaTheme.alpha(0xFF000000 | (ModConfigData.navenBgColor() & 0xFFFFFF), alpha);
    }

    private void blurFloatFor(float x, float y, float w, float h, float radius) {
        if (w <= 1.0f || h <= 1.0f) return;
        if (!ModConfigData.rendererEnabled() || !ModConfigData.navenBlur() || !NvPostFx.available()) return;
        if (currentPose == null || currentGraphics == null) return;
        currentGraphics.flush();
        NvPostFx.blur(currentPose, x, y, w, h, liveBlurStrength(), radius);
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    private static float approach(float current, float target, float dt, float speed) {
        float t = 1.0f - (float) Math.exp(-Math.max(0.0f, dt) * speed);
        return current + (target - current) * t;
    }

    private static List<Category> buildCategories() {
        List<Category> list = new ArrayList<>();
        for (Category category : Category.values()) {
            for (Module module : ModuleManager.get().getModules()) {
                if (module.category == category) {
                    list.add(category);
                    break;
                }
            }
        }
        return List.copyOf(list);
    }

    private static List<Module> modulesOf(Category category) {
        List<Module> list = new ArrayList<>();
        for (Module module : ModuleManager.get().getModules()) {
            if (module.category == category) list.add(module);
        }
        return list;
    }

    private static List<Value<?>> valuesOf(Module module) {
        List<Value<?>> list = new ArrayList<>();
        for (Value<?> value : module.values) {
            if (!value.isHidden()) list.add(value);
        }
        return list;
    }

    private static String displayName(Category category) {
        return Lang.tr(category.key());
    }

    private static String displayName(Module module) {
        return Lang.byZh(module.name);
    }

    private static String displayName(Value<?> value) {
        return Lang.byZh(value.name);
    }

    private static String displayName(String raw) {
        return Lang.byZh(raw);
    }
}
