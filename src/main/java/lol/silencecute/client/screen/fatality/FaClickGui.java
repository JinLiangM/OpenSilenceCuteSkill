package lol.silencecute.client.screen.fatality;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.blaze3d.platform.InputConstants;
import lol.silencecute.Client;
import lol.silencecute.client.AccessClient;
import lol.silencecute.client.lang.Lang;
import lol.silencecute.modules.Category;
import lol.silencecute.client.screen.HudEditScreen;
import lol.silencecute.modules.Module;
import lol.silencecute.modules.ModuleManager;
import lol.silencecute.config.ModConfigData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class FaClickGui extends Screen {
    static final String CLIENT_NAME = "CLICKGUI";
    static final String CLIENT_VERSION = resolveVersion();

    private static final float TAB_H = 20.5f;
    private static final float TAB_PAD = 7.5f;
    private static final float TAB_ICON = 10.0f;
    private static final float TAB_GAP = 4.0f;

    private static final float TAB_DX = 0.0f;
    private static final float TAB_DY = 0.0f;
    private static final float AVATAR = 23.5f;
    private static final float SIDE_ITEM_H = 19.0f;
    private static final float SEARCH_W = 150.0f;
    private static final float GRIP = 9.0f;

    private static final float SHADE = 6.0f;

    private static float ratioX = Float.NaN, ratioY = Float.NaN, savedW = Float.NaN, savedH = Float.NaN;
    private static Category savedCategory = Category.RENDER;

    private static Category pendingCategory;

    public static void requestCategory(Category target) {
        pendingCategory = target;
    }

    private final FaContext ctx = new FaContext(this);

    private final FaDraw draw = new NavenFaDraw();
    private final Screen parent;
    private final Map<Category, List<FaGroup>> groups = new EnumMap<>(Category.class);
    private final Map<Category, View> views = new EnumMap<>(Category.class);
    private final View searchView = new View();
    private List<Category> categories = List.of();
    private Category category = savedCategory;

    private float scale = 1.0f, sw, sh;
    private float wx, wy, ww, wh;
    private boolean placed;
    private boolean mouseDown;
    private final FaMotion openMotion = new FaMotion(0.0f);

    private Screen pendingScreen;

    private boolean closing;
    private final FaMotion tabX = new FaMotion(Float.NaN);
    private final FaMotion tabW = new FaMotion(0.0f);
    private final FaMotion contentFade = new FaMotion(1.0f);
    private final FaMotion searchOpen = new FaMotion(0.0f);
    private final FaMotion toastAlpha = new FaMotion(0.0f);
    private final FaMotion scrollbar = new FaMotion(0.0f);
    private final FaMotion[] footerHover = {new FaMotion(0.0f), new FaMotion(0.0f), new FaMotion(0.0f), new FaMotion(0.0f)};
    private final Map<Category, FaMotion> tabHover = new EnumMap<>(Category.class);

    private final List<FaPopup> popups = new ArrayList<>();
    private Object suppressedOwner;
    private FaKeySink focus;
    private final FaTextInput search = new FaTextInput(48);
    private boolean searchActive;
    private String query = "";
    private String toast = "";
    private long toastAt;

    private int drag;
    private float dragOffsetX, dragOffsetY, startW, startH, startX, startY;
    private long lastNanos;
    private String layoutKey = "";

    private final FaKeySink searchSink = new FaKeySink() {
        @Override
        public boolean keyPressed(int key, int modifiers) {
            return search.keyPressed(key, modifiers);
        }

        @Override
        public boolean charTyped(char character) {
            return search.charTyped(character);
        }
    };

    private static final class View {
        float scroll;
        final FaMotion scrollMotion = new FaMotion(0.0f);
        final FaMotion barY = new FaMotion(Float.NaN);
        float sideScroll;
        final FaMotion sideScrollMotion = new FaMotion(0.0f);
        Module selected;
        List<FaGroup> shown = List.of();
        float maxScroll;
    }

    public FaClickGui(Screen parent) {
        super(Component.literal(CLIENT_NAME));
        this.parent = parent;
        Category pending = pendingCategory;
        pendingCategory = null;
        if (pending != null) {
            this.category = pending;
            savedCategory = pending;
        }
        search.onChange(this::setQuery)
                .onSubmit(text -> focus(null))
                .onCancel(() -> {
                    if (search.text().isEmpty()) closeSearch();
                    else {
                        search.set("");
                        setQuery("");
                    }
                });
    }

    private static String resolveVersion() {
        try {
            return net.minecraftforge.fml.ModList.get().getModContainerById(Client.MOD_ID)
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElse("1.0.0");
        } catch (Throwable ignored) {
            return "1.0.0";
        }
    }

    @Override
    protected void init() {
        super.init();
        AccessClient.query();
        syncModules();
        if (category == Category.ADMIN && !AccessClient.isAdmin()) {
            category = Category.RENDER;
        }
        updateScale();
        float maxW = Math.max(FaTheme.MIN_W, sw - 16.0f), maxH = Math.max(FaTheme.MIN_H, sh - 16.0f);
        if (hasLayout()) {
            ww = clamp(Math.max(savedW, FaTheme.DEFAULT_W), FaTheme.MIN_W, maxW);
            wh = clamp(Math.max(savedH, FaTheme.DEFAULT_H), FaTheme.MIN_H, maxH);
            wx = ratioX * sw;
            wy = ratioY * sh;
        } else if (!placed) {
            ww = Math.min(FaTheme.DEFAULT_W, maxW);
            wh = Math.min(FaTheme.DEFAULT_H, maxH);
            wx = (sw - ww) * 0.5f;
            wy = (sh - wh) * 0.5f;
        }
        fitWindow();
        placed = true;
        layoutKey = "";
    }

    private static boolean hasLayout() {
        return Float.isFinite(ratioX) && Float.isFinite(ratioY) && Float.isFinite(savedW) && Float.isFinite(savedH);
    }

    private void updateScale() {
        if (width <= 0 || height <= 0) return;
        float fit = Math.min(1.0f, Math.min((width - 16.0f) / FaTheme.DEFAULT_W,
                (height - 16.0f) / FaTheme.DEFAULT_H));
        float next = Math.max(0.3f, fit);
        if (next == scale && sw > 0.0f) return;
        if (placed) {
            float centerX = (wx + ww * 0.5f) * scale, centerY = (wy + wh * 0.5f) * scale;
            wx = centerX / next - ww * 0.5f;
            wy = centerY / next - wh * 0.5f;
        }
        scale = next;
        sw = width / scale;
        sh = height / scale;
        if (placed) fitWindow();
    }

    private void syncModules() {
        if (groups.isEmpty()) loadModules(ModuleManager.get().getModules());
    }

    private void loadModules(List<Module> visible) {
        groups.clear();
        List<Category> available = new ArrayList<>();
        boolean adminAllowed = AccessClient.isAdmin();
        for (Category value : Category.values()) {
            List<FaGroup> list = new ArrayList<>();
            if (value != Category.ADMIN || adminAllowed) {
                for (Module module : visible) if (module.category == value) list.add(new FaGroup(module));
            }
            if (list.isEmpty() && value != Category.ADMIN) continue;
            groups.put(value, list);
            views.computeIfAbsent(value, ignored -> new View());
            tabHover.computeIfAbsent(value, ignored -> new FaMotion(0.0f));
            available.add(value);
        }
        categories = List.copyOf(available);
        if (!categories.contains(category)) category = categories.isEmpty() ? Category.RENDER : categories.get(0);
        layoutKey = "";
        if (!query.isEmpty()) setQuery(query);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void beginClose() {
        if (closing) return;
        closing = true;
        drag = 0;
        if (focus != null) focus(null);
        closeAllPopups();
    }

    @Override
    public void onClose() {
        if (focus != null) focus(null);
        saveLayout();
        GuiTransition.reset();
        if (minecraft != null) {
            minecraft.setScreen(pendingScreen != null ? pendingScreen : parent);
        }
    }

    boolean isPopupOpen(Object owner) {
        for (FaPopup popup : popups) if (popup.owner() == owner && !popup.closing()) return true;
        return false;
    }

    void togglePopup(Object owner, Supplier<FaPopup> factory, float ax, float ay, float aw, float ah, boolean beside) {
        if (owner == suppressedOwner) return;
        for (FaPopup popup : popups) {
            if (popup.owner() == owner && !popup.closing()) {
                closeFrom(popups.indexOf(popup));
                return;
            }
        }
        FaPopup popup = factory.get();
        if (beside) popup.placeBeside(ax, ay, aw, ah, 0, 0, sw, sh);
        else if (ay >= wy + wh - FaTheme.FOOTER_H - 1.0f) popup.placeAbove(ax, ay, 0, 0, sw, sh);
        else popup.place(ax, ay, aw, ah, 0, 0, sw, sh);
        popups.add(popup);
    }

    private void closeFrom(int index) {
        for (int i = popups.size() - 1; i >= index; i--) popups.get(i).close();
    }

    private void closeAllPopups() {
        closeFrom(0);
    }

    void focus(FaKeySink sink) {
        if (focus == sink) return;
        FaKeySink previous = focus;
        focus = sink;
        if (previous != null) previous.focusLost();
    }

    void toast(String message) {
        toast = message;
        toastAt = System.currentTimeMillis();
    }

    void saveLayout() {
        if (sw > 0 && sh > 0) {
            ratioX = wx / sw;
            ratioY = wy / sh;
        }
        savedW = ww;
        savedH = wh;
        savedCategory = category;
        try {
            ModConfigData.SPEC.save();
        } catch (Exception ignored) {

        }
    }

    void resetLayout() {
        ratioX = Float.NaN;
        ratioY = Float.NaN;
        savedW = Float.NaN;
        savedH = Float.NaN;
        placed = false;
        layoutKey = "";
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        FaTheme.refresh();
        long now = System.nanoTime();
        float rawDt = lastNanos == 0L ? 1.0f / 60.0f : (now - lastNanos) / 1.0e9f;
        lastNanos = now;

        float dt = rawDt > 0.15f ? 0.0f : Math.min(0.1f, rawDt);
        syncModules();

        if (GuiTransition.isSwitching()) openMotion.snap(1.0f);

        if (!mouseDown) updateScale();
        float mx = mouseX / scale, my = mouseY / scale;
        try {
            draw.begin(graphics);

            draw.setLowQuality(drag != 0);
            try (var scaled = draw.push()) {
                draw.scale(scale);
                drawWithTransition(draw, mx, my, dt);
            }
        } finally {

            draw.closeAllClips();
        }
        GuiTransition.tick();
    }

    private void drawWithTransition(FaDraw canvas, float mx, float my, float dt) {
        float shrink = GuiTransition.scale();
        if (shrink >= 0.999f) {
            drawFrame(canvas, mx, my, dt);
            return;
        }
        float cw = Math.max(0.0f, ww * shrink);
        float ch = Math.max(0.0f, wh * shrink);
        float cx = wx + (ww - cw) * 0.5f;
        float cy = wy + (wh - ch) * 0.5f;
        try (FaDraw.Scope clip = canvas.clip(cx, cy, cw, ch)) {
            drawFrame(canvas, mx, my, dt);
        }
    }

    private void drawFrame(FaDraw canvas, float mouseX, float mouseY, float dt) {

        if (closing && openMotion.value() <= 0.02f) {
            onClose();
            return;
        }
        ctx.draw = canvas;
        ctx.dt = dt;
        popups.removeIf(FaPopup::finished);
        ctx.mouseX = mouseX;
        ctx.mouseY = mouseY;
        ctx.hoverEnabled = !closing && topPopupAt(mouseX, mouseY) == null && drag == 0;
        float open = openMotion.toward(closing ? 0.0f : 1.0f, ctx.dt, 16.0f);
        try (var ignored = canvas.push()) {
            float lift = (1.0f - open) * 8.0f;
            canvas.translate(0.0f, lift);

            float shrink = 0.92f + 0.08f * open;
            if (shrink < 0.9995f) {
                float cx = wx + ww * 0.5f;
                float cy = wy + wh * 0.5f;
                canvas.translate(cx, cy);
                canvas.scale(shrink);
                canvas.translate(-cx, -cy);
            }
            canvas.opacity(open);
            drawWindow(lift, open);
            for (int i = 0; i < popups.size(); i++) {
                FaPopup popup = popups.get(i);
                ctx.hoverEnabled = popup.contains(mouseX, mouseY) && topPopupAt(mouseX, mouseY) == popup;
                popup.render(ctx);
            }
        }
    }

    private void drawWindow(float lift, float open) {
        FaDraw canvas = ctx.draw;

        canvas.rect(wx, wy, ww, wh, FaTheme.RADIUS, FaTheme.WINDOW);
        drawSidebar();
        drawContent();
        drawHeader(lift, open);
        drawFooter();
    }

    private float logoRight() {
        return wx + 16.0f + FaTheme.logo().width(CLIENT_NAME);
    }

    private float[] tabBounds(int index, boolean iconsOnly) {
        FaFont font = FaTheme.tab();
        float x = Math.max(wx + 103.0f, logoRight() + 14.0f);
        for (int i = 0; i <= index; i++) {
            Category value = categories.get(i);
            float labelW = iconsOnly ? 0.0f : TAB_GAP + font.width(Lang.tr(value.key()).toUpperCase(Locale.ROOT));
            float w = TAB_PAD + TAB_ICON + labelW + TAB_PAD;
            if (i == index) return new float[]{x, wy + (FaTheme.HEADER_H - TAB_H) * 0.5f, w, TAB_H};
            x += w + 2.0f;
        }
        return new float[]{x, wy, 0, TAB_H};
    }

    private boolean tabsIconsOnly() {
        if (categories.isEmpty()) return false;
        float[] last = tabBounds(categories.size() - 1, false);
        return last[0] + last[2] > userLeft() - 10.0f;
    }

    private float userLeft() {
        UserInfo info = userInfo();
        float textW = Math.max(FaTheme.label().width(info.name), FaTheme.small().width(info.label + info.value));
        return wx + ww - 18.0f - AVATAR - 8.0f - textW;
    }

    private void drawHeader(float lift, float open) {
        FaDraw canvas = ctx.draw;
        float h = FaTheme.HEADER_H;

        canvas.vertical(wx, wy + h, ww, SHADE, FaTheme.SHADE_DARK, FaTheme.SHADE_CLEAR);
        canvas.rect(wx, wy, ww, FaTheme.RADIUS * 2.0f, FaTheme.RADIUS, FaTheme.HEADER_TOP);
        canvas.vertical(wx, wy + FaTheme.RADIUS, ww, h - FaTheme.RADIUS,
                FaTheme.mix(FaTheme.HEADER_TOP, FaTheme.HEADER, 0.1f), FaTheme.HEADER);
        canvas.rect(wx, wy + h - 0.5f, ww, 0.5f, FaTheme.DIVIDER);
        float mid = wy + h * 0.5f;
        canvas.textMid(CLIENT_NAME, wx + 16.0f, mid + 0.5f, FaTheme.logo(), FaTheme.LOGO);

        boolean iconsOnly = tabsIconsOnly();
        FaFont font = FaTheme.tab();
        int activeIndex = categories.indexOf(category);
        if (activeIndex >= 0 && query.isEmpty()) {
            float[] active = tabBounds(activeIndex, iconsOnly);
            float relX = active[0] - wx;
            if (Float.isNaN(tabX.value())) {
                tabX.snap(relX);
                tabW.snap(active[2]);
            }
            float px = wx + tabX.toward(relX, ctx.dt, 18.0f);
            float pw = tabW.toward(active[2], ctx.dt, 18.0f);
            canvas.rect(px, active[1], pw, TAB_H, 3.0f, FaTheme.TAB_ACTIVE);
        }
        for (int i = 0; i < categories.size(); i++) {
            Category value = categories.get(i);
            float[] b = tabBounds(i, iconsOnly);
            boolean selected = value == category && query.isEmpty();
            float hover = tabHover.get(value).toward(ctx.hovered(b[0], b[1], b[2], b[3]) ? 1.0f : 0.0f, ctx.dt, 16.0f);
            int textColor = selected ? FaTheme.TAB_TEXT_ACTIVE
                    : FaTheme.mix(FaTheme.TAB_TEXT, FaTheme.TAB_TEXT_HOVER, hover);
            int iconColor = selected ? FaTheme.ACCENT : textColor;
            canvas.icon(FaTheme.icon(value), b[0] + TAB_PAD + TAB_ICON * 0.5f + TAB_DX, mid + TAB_DY, TAB_ICON,
                    iconColor);
            if (!iconsOnly) {
                canvas.textMid(Lang.tr(value.key()).toUpperCase(Locale.ROOT),
                        b[0] + TAB_PAD + TAB_ICON + TAB_GAP + TAB_DX,
                        mid - 1.25f + TAB_DY, font, textColor);
            }
        }

        UserInfo info = userInfo();
        float ax = wx + ww - 18.0f - AVATAR, ay = mid - AVATAR * 0.5f;
        float textRight = ax - 7.0f;
        canvas.textRightMid(info.name, textRight, mid - 5.0f, FaTheme.label(), FaTheme.USER_NAME);
        FaFont small = FaTheme.small();
        float valueW = small.width(info.value);
        canvas.textRightMid(info.value, textRight, mid + 5.5f, small, FaTheme.ACCENT);
        canvas.textRightMid(info.label, textRight - valueW, mid + 5.5f, small, FaTheme.MUTED);
        drawAvatar(ax, ay, AVATAR, info);
    }

    private record UserInfo(String name, String label, String value) {
    }

    private UserInfo userInfo() {
        String name = minecraft != null && minecraft.player != null
                ? minecraft.player.getGameProfile().getName()
                : minecraft != null && minecraft.getUser() != null ? minecraft.getUser().getName() : "player";
        return new UserInfo(name, Lang.tr("info.operator_prefix"), ModConfigData.operator().name());
    }

    private void drawAvatar(float x, float y, float size, UserInfo info) {
        FaDraw canvas = ctx.draw;
        canvas.rect(x, y, size, size, size * 0.5f, FaTheme.PORTRAIT_BG);
        if (minecraft != null && minecraft.player != null) {

            canvas.skinFace(minecraft.player, x, y, size);

            float k = size * 0.16f;
            canvas.outline(x - k, y - k, size + k * 2.0f, size + k * 2.0f, size * 0.5f + k, k,
                    FaTheme.mix(FaTheme.HEADER_TOP, FaTheme.HEADER, 0.5f));
        } else {
            canvas.textCenter(info.name.isEmpty() ? "?" : info.name.substring(0, 1).toUpperCase(Locale.ROOT),
                    x + size * 0.5f, y + size * 0.5f, FaTheme.title(), FaTheme.LABEL);
        }
    }

    private float contentX() { return wx + FaTheme.SIDEBAR_W; }
    private float contentY() { return wy + FaTheme.HEADER_H; }
    private float contentW() { return ww - FaTheme.SIDEBAR_W; }
    private float contentH() { return wh - FaTheme.HEADER_H - FaTheme.FOOTER_H; }

    private View view() {
        return query.isEmpty() ? views.computeIfAbsent(category, ignored -> new View()) : searchView;
    }

    private List<FaGroup> currentGroups() {
        return query.isEmpty() ? groups.getOrDefault(category, List.of()) : searchView.shown;
    }

    private List<FaBox> currentBoxes() {
        return new ArrayList<>(currentGroups());
    }

    boolean popupOver(float x, float y, float w, float h) {
        for (FaPopup popup : popups) {
            if (!popup.closing() && popup.x < x + w && popup.x + popup.w > x && popup.y < y + h && popup.y + popup.h > y) {
                return true;
            }
        }
        return false;
    }

    private float sideTop() {
        return contentY() + 11.0f;
    }

    private void drawSidebar() {
        FaDraw canvas = ctx.draw;
        View view = view();
        List<FaGroup> list = currentGroups();
        float top = sideTop();
        float height = contentH() - 16.0f;
        float maxSide = Math.max(0.0f, list.size() * SIDE_ITEM_H - height);
        view.sideScroll = clamp(view.sideScroll, 0.0f, maxSide);
        float offset = view.sideScrollMotion.toward(view.sideScroll, ctx.dt, 16.0f);
        Module active = activeModule(view, list);
        FaFont font = FaTheme.label();
        try (var clip = canvas.clip(wx, top - 2.0f, FaTheme.SIDEBAR_W, height + 4.0f)) {
            int activeIndex = -1;
            for (int i = 0; i < list.size(); i++) {
                FaGroup group = list.get(i);
                float iy = top + i * SIDE_ITEM_H - offset;
                if (group.module == active) activeIndex = i;
                if (iy + SIDE_ITEM_H < top - 2.0f || iy > top + height) continue;
                boolean hovered = ctx.hovered(wx, iy, FaTheme.SIDEBAR_W, SIDE_ITEM_H);
                int color = group.module == active ? FaTheme.SIDE_TEXT_ACTIVE
                        : hovered ? FaTheme.LABEL
                        : group.module.state ? FaTheme.SIDE_TEXT_ENABLED : FaTheme.SIDE_TEXT;
                canvas.textMid(FaTheme.fit(group.title(), font, FaTheme.SIDEBAR_W - 22.0f), wx + 16.0f,
                        iy + SIDE_ITEM_H * 0.5f, font, color);
            }
            if (activeIndex >= 0) {
                float target = activeIndex * SIDE_ITEM_H - offset;
                if (Float.isNaN(view.barY.value())) view.barY.snap(target);
                float barY = top + view.barY.toward(target, ctx.dt, 18.0f);
                canvas.rect(wx + 9.0f, barY + (SIDE_ITEM_H - 16.0f) * 0.5f, 2.0f, 16.0f, 1.0f, FaTheme.ACCENT);
            }
        }
    }

    private Module activeModule(View view, List<FaGroup> list) {
        if (list.isEmpty()) return null;
        if (view.selected != null) {
            for (FaGroup group : list) if (group.module == view.selected) return view.selected;
        }

        FaGroup best = list.get(0);
        for (FaGroup group : list) {
            if (group.targetY() <= view.scroll + 18.0f && group.targetY() > best.targetY()) best = group;
        }
        return best.module;
    }

    private int columns(float contentWidth) {
        return Math.max(1, Math.min(3, (int) Math.floor((contentWidth - FaTheme.CONTENT_PAD + FaTheme.COLUMN_GAP)
                / (FaTheme.MIN_COLUMN_W + FaTheme.COLUMN_GAP))));
    }

    private void drawContent() {
        FaDraw canvas = ctx.draw;
        View view = view();
        List<FaBox> list = currentBoxes();
        float cx = contentX(), cy = contentY(), cw = contentW(), ch = contentH();
        int columns = columns(cw);

        float columnW = (cw - FaTheme.CONTENT_PAD - FaTheme.COLUMN_GAP * (columns - 1)) / columns;
        String key = (query.isEmpty() ? category.name() : "?" + query) + "|" + columns + "|" + Math.round(columnW);
        boolean snap = !key.equals(layoutKey);
        layoutKey = key;

        float[] columnHeights = new float[columns];
        for (FaBox group : list) {
            int column = 0;
            for (int i = 1; i < columns; i++) if (columnHeights[i] < columnHeights[column] - 0.5f) column = i;
            group.place(column * (columnW + FaTheme.COLUMN_GAP), columnHeights[column], snap);
            columnHeights[column] += group.targetHeight() + FaTheme.GROUP_GAP;
        }
        float total = 0.0f;
        for (float height : columnHeights) total = Math.max(total, height);
        view.maxScroll = Math.max(0.0f, total - FaTheme.GROUP_GAP + FaTheme.CONTENT_PAD * 2 - ch);
        view.scroll = clamp(view.scroll, 0.0f, view.maxScroll);
        if (snap) view.scrollMotion.snap(view.scroll);
        float scroll = view.scrollMotion.toward(view.scroll, ctx.dt, 15.0f);

        float fade = contentFade.toward(1.0f, ctx.dt, 12.0f);
        try (var clip = canvas.clip(cx, cy, cw, ch)) {
            try (var faded = canvas.push()) {
                canvas.translate(0.0f, (1.0f - fade) * 6.0f);
                canvas.opacity(fade);
                float originY = cy + FaTheme.CONTENT_PAD - scroll;
                for (FaBox box : list) box.render(ctx, cx, originY, columnW, cy, cy + ch, 1.0f);
                if (list.isEmpty()) {
                    canvas.textCenter(query.isEmpty() ? Lang.tr("gui.search.empty") : Lang.tr("gui.search.nothing") + query + "\"",
                            cx + cw * 0.5f, cy + ch * 0.4f, FaTheme.label(), FaTheme.MUTED);
                }
            }
        }
        if (view.maxScroll > 0.5f) {

            boolean moving = Math.abs(scroll - view.scroll) > 0.5f;
            boolean near = ctx.hovered(cx + cw - 12.0f, cy, 12.0f, ch);
            float shown = scrollbar.toward(moving || near ? 1.0f : 0.35f, ctx.dt, moving ? 20.0f : 6.0f);
            float trackH = ch - 12.0f;
            float thumbH = Math.max(18.0f, trackH * ch / (ch + view.maxScroll));
            float thumbY = cy + 6.0f + (trackH - thumbH) * (scroll / view.maxScroll);
            canvas.rect(cx + cw - 5.0f, thumbY, 2.0f, thumbH, 1.0f, FaTheme.alpha(FaTheme.SCROLLBAR, shown));
        }
    }

    private float footerIconX(int index) {
        return wx + 16.0f + index * 23.0f;
    }

    private void drawFooter() {
        FaDraw canvas = ctx.draw;
        float fy = wy + wh - FaTheme.FOOTER_H;
        canvas.vertical(wx, fy - SHADE, ww, SHADE, FaTheme.SHADE_CLEAR, FaTheme.SHADE_DARK);
        canvas.rect(wx, fy, ww, FaTheme.FOOTER_H, FaTheme.RADIUS, FaTheme.FOOTER);
        canvas.rect(wx, fy, ww, FaTheme.RADIUS, FaTheme.FOOTER);
        canvas.rect(wx, fy, ww, 0.5f, FaTheme.DIVIDER);
        float mid = fy + FaTheme.FOOTER_H * 0.5f;
        String[] icons = {FaTheme.I_SEARCH, FaTheme.I_SAVE, FaTheme.I_HUD};
        for (int i = 0; i < icons.length; i++) {
            float ix = footerIconX(i);
            float hover = footerHover[i].toward(ctx.hovered(ix - 9, fy, 18, FaTheme.FOOTER_H) ? 1.0f : 0.0f, ctx.dt, 16.0f);
            boolean active = i == 0 && searchActive;
            int color = active ? FaTheme.ACCENT : FaTheme.mix(FaTheme.ICON, FaTheme.ICON_HOVER, hover);
            canvas.icon(icons[i], ix, mid, 11.0f, color);
        }
        float infoX = wx + ww - 19.0f;
        float infoHover = footerHover[3].toward(ctx.hovered(infoX - 9, fy, 18, FaTheme.FOOTER_H) ? 1.0f : 0.0f, ctx.dt, 16.0f);
        canvas.icon(FaTheme.I_INFO, infoX, mid, 11.0f, isPopupOpen(FaTheme.I_INFO) ? FaTheme.ACCENT
                : FaTheme.mix(FaTheme.ICON, FaTheme.ICON_HOVER, infoHover));

        float open = searchOpen.toward(searchActive ? 1.0f : 0.0f, ctx.dt, 16.0f);
        float fieldX = footerIconX(2) + 16.0f;
        if (open > 0.01f) {
            float fieldW = SEARCH_W * open;
            canvas.rect(fieldX, mid - 7.0f, fieldW, 14.0f, FaTheme.FIELD_RADIUS, FaTheme.FIELD);
            try (var clip = canvas.clip(fieldX, mid - 8.0f, fieldW, 16.0f)) {
                search.render(canvas, fieldX + 6.0f, mid, fieldW - 12.0f, FaTheme.field(), FaTheme.LABEL,
                        focus == searchSink, Lang.tr("gui.search.hint"), FaTheme.MUTED);
            }
        }
        float toastVisible = toastAlpha.toward(System.currentTimeMillis() - toastAt < 2200L && !toast.isEmpty()
                ? 1.0f : 0.0f, ctx.dt, 10.0f);
        if (toastVisible > 0.01f) {
            float tx = fieldX + SEARCH_W * Math.max(open, 0.0f) + (open > 0.01f ? 12.0f : 0.0f);
            canvas.textMid(toast, tx, mid, FaTheme.field(), FaTheme.alpha(FaTheme.FIELD_TEXT_HOVER, toastVisible));
        }

    }

    private void openSearch() {
        searchActive = true;
        search.focused();
        focus(searchSink);
    }

    private void closeSearch() {
        searchActive = false;
        search.set("");
        setQuery("");
        focus(null);
    }

    private void setQuery(String text) {
        String next = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        boolean changed = !next.equals(query);
        query = next;
        if (query.isEmpty()) {
            for (List<FaGroup> list : groups.values()) for (FaGroup group : list) group.filter("");
            searchView.shown = List.of();
        } else {
            List<FaGroup> matches = new ArrayList<>();
            for (Category value : categories) {
                for (FaGroup group : groups.getOrDefault(value, List.of())) if (group.filter(query)) matches.add(group);
            }
            searchView.shown = matches;
        }
        if (changed) {
            searchView.scroll = 0.0f;
            searchView.selected = null;
            contentFade.snap(0.6f);
            closeAllPopups();
        }
    }

    private FaPopup topPopupAt(double mx, double my) {
        for (int i = popups.size() - 1; i >= 0; i--) if (popups.get(i).contains(mx, my)) return popups.get(i);
        return null;
    }

    @Override
    public boolean mouseClicked(double px, double py, int button) {
        if (closing) return true;

        if (GuiTransition.isSwitching()) return true;
        double mx = px / scale, my = py / scale;
        mouseDown = true;
        FaPopup popup = topPopupAt(mx, my);
        if (popup != null) {

            closeFrom(popups.indexOf(popup) + 1);
            focus(null);
            popup.mouseClicked(ctx, mx, my, button);
            return true;
        }

        Object topOwner = null;
        for (int i = popups.size() - 1; i >= 0; i--) {
            if (!popups.get(i).closing()) {
                topOwner = popups.get(i).owner();
                break;
            }
        }
        suppressedOwner = topOwner;
        closeAllPopups();

        FaKeySink listening = focus;
        boolean captured = listening != null && listening.mousePressed(mx, my, button);
        focus(null);
        if (captured) return true;
        try {
            return handleClick(mx, my, button) || super.mouseClicked(px, py, button);
        } finally {
            suppressedOwner = null;
        }
    }

    private boolean handleClick(double mx, double my, int button) {

        float fy = wy + wh - FaTheme.FOOTER_H;
        if (FaContext.inside(mx, my, wx, fy, ww, FaTheme.FOOTER_H)) {
            if (button != 0) return true;
            if (FaContext.inside(mx, my, footerIconX(0) - 9, fy, 18, FaTheme.FOOTER_H)) {
                if (searchActive) closeSearch();
                else openSearch();
            } else if (FaContext.inside(mx, my, footerIconX(1) - 9, fy, 18, FaTheme.FOOTER_H)) {
                ModConfigData.SPEC.save();
                toast(Lang.tr("gui.config.saved"));
            } else if (FaContext.inside(mx, my, footerIconX(2) - 9, fy, 18, FaTheme.FOOTER_H)) {

                ModConfigData.SPEC.save();
                if (minecraft != null) {
                    pendingScreen = new HudEditScreen();
                    beginClose();
                }
            } else if (FaContext.inside(mx, my, wx + ww - 28, fy, 18, FaTheme.FOOTER_H)) {
                togglePopup(FaTheme.I_INFO, () -> new FaInfoPopup(FaTheme.I_INFO, this), wx + ww - 196, fy, 18,
                        FaTheme.FOOTER_H, false);
            } else if (searchActive && FaContext.inside(mx, my, footerIconX(2) + 16, fy, SEARCH_W, FaTheme.FOOTER_H)) {
                search.focused();
                focus(searchSink);
            }
            return true;
        }

        if (FaContext.inside(mx, my, wx, wy, ww, FaTheme.HEADER_H)) {
            boolean iconsOnly = tabsIconsOnly();
            for (int i = 0; i < categories.size(); i++) {
                float[] b = tabBounds(i, iconsOnly);
                if (FaContext.inside(mx, my, b[0], b[1], b[2], b[3])) {
                    if (button == 0) switchCategory(categories.get(i));
                    return true;
                }
            }
            if (button == 0) {
                drag = 1;
                dragOffsetX = (float) mx - wx;
                dragOffsetY = (float) my - wy;
            }
            return true;
        }
        if (!FaContext.inside(mx, my, wx, wy, ww, wh)) return false;

        if (FaContext.inside(mx, my, wx, contentY(), FaTheme.SIDEBAR_W, contentH())) {
            View view = view();
            List<FaGroup> list = currentGroups();
            float top = sideTop() - view.sideScrollMotion.value();
            int index = (int) Math.floor((my - top) / SIDE_ITEM_H);
            if (button == 0 && index >= 0 && index < list.size()) {
                FaGroup group = list.get(index);
                if (group.module instanceof lol.silencecute.modules.impl.misc.OperatorModule
                        && AccessClient.isLocked()) {
                    lol.silencecute.client.render.hud.notification.NotificationRea1.error("鉴权未通过！");
                    return true;
                }

                view.selected = group.module;
                view.scroll = clamp(group.targetY(), 0.0f, view.maxScroll);
                group.flash();
            }
            return true;
        }

        if (FaContext.inside(mx, my, contentX(), contentY(), contentW(), contentH())) {
            for (FaBox box : currentBoxes()) if (box.mouseClicked(ctx, mx, my, button)) return true;
        }
        return true;
    }

    private void switchCategory(Category next) {
        if (next == Category.ADMIN && !AccessClient.isAdmin()) {
            lol.silencecute.client.render.hud.notification.NotificationRea1.error("鉴权未通过！");
            return;
        }
        if (searchActive) closeSearch();
        if (next == category) return;
        category = next;
        contentFade.snap(0.0f);
        View view = view();
        view.selected = null;
        layoutKey = "";
    }

    @Override
    public boolean mouseDragged(double px, double py, int button, double dragX, double dragY) {
        double mx = px / scale, my = py / scale;
        if (drag == 1) {
            wx = (float) mx - dragOffsetX;
            wy = (float) my - dragOffsetY;
            keepOnScreen();
            return true;
        }
        for (FaPopup popup : popups) popup.mouseDragged(ctx, mx, my);
        for (FaBox box : currentBoxes()) box.mouseDragged(ctx, mx, my);
        return true;
    }

    @Override
    public boolean mouseReleased(double px, double py, int button) {
        mouseDown = false;
        if (drag != 0) {
            drag = 0;
            saveLayout();
        }
        for (FaPopup popup : popups) popup.mouseReleased();
        for (FaBox box : currentBoxes()) box.mouseReleased();
        return super.mouseReleased(px, py, button);
    }

    @Override
    public boolean mouseScrolled(double px, double py, double delta) {
        if (closing) return true;
        double mx = px / scale, my = py / scale;
        FaPopup popup = topPopupAt(mx, my);
        if (popup != null) return popup.mouseScrolled(ctx, mx, my, delta);
        View view = view();
        if (FaContext.inside(mx, my, wx, contentY(), FaTheme.SIDEBAR_W, contentH())) {
            view.sideScroll -= (float) delta * SIDE_ITEM_H * 1.5f;
            return true;
        }
        if (!FaContext.inside(mx, my, contentX(), contentY(), contentW(), contentH())) return false;

        if (minecraft != null && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT)) {
            for (FaBox box : currentBoxes()) if (box.mouseScrolled(ctx, mx, my, delta)) return true;
        }
        closeAllPopups();
        view.scroll = clamp(view.scroll - (float) delta * 34.0f, 0.0f, view.maxScroll);
        view.selected = null;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (focus != null && focus.keyPressed(key, modifiers)) return true;
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            for (int i = popups.size() - 1; i >= 0; i--) {
                if (!popups.get(i).closing()) {
                    popups.get(i).close();
                    return true;
                }
            }
            if (searchActive) {
                closeSearch();
                return true;
            }
            beginClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_F && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            openSearch();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (focus != null && focus.charTyped(character)) return true;
        return super.charTyped(character, modifiers);
    }

    private void fitWindow() {
        ww = clamp(ww, Math.min(FaTheme.MIN_W, sw - 8.0f), Math.max(FaTheme.MIN_W, sw - 8.0f));
        wh = clamp(wh, Math.min(FaTheme.MIN_H, sh - 8.0f), Math.max(FaTheme.MIN_H, sh - 8.0f));
        keepOnScreen();
    }

    private void keepOnScreen() {
        wx = clamp(wx, 4.0f - ww + 60.0f, sw - 60.0f);
        wy = clamp(wy, 4.0f, sh - FaTheme.HEADER_H);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(Math.max(min, max), value));
    }
}