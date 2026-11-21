package lol.silencecute.modules;

import java.util.ArrayList;
import java.util.List;

import lol.silencecute.client.lang.Lang;
import lol.silencecute.client.render.hud.notification.NotificationRea1;
import lol.silencecute.client.screen.Value;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

public class Module {

    public String name;

    public String description;

    public Category category;

    public final List<Value<?>> values = new ArrayList<>();

    public boolean state;

    public boolean toggleable;

    public boolean enableRow = true;

    public float toggleAnimation;

    public int key = -1;

    public Runnable onToggle;

    private String suffix;

    public Module() {
    }

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    public Module(String name, Category category) {
        this(name, "", category);
    }

    protected void initModule() {
        ModuleInfo info = getClass().getAnnotation(ModuleInfo.class);
        if (info != null) {
            this.name = info.name();
            this.description = info.description();
            this.category = info.category();
        }
    }

    public Module addValue(Value<?> value) {
        this.values.add(value);
        return this;
    }

    public Module whenToggled(Runnable action) {
        this.onToggle = action;
        return this;
    }

    public void toggle() {
        setState(!state);
    }

    public void setState(boolean newState) {
        if (this.state == newState) return;
        this.state = newState;
        playClickSound();
        if (onToggle != null) {
            try {
                onToggle.run();
            } catch (Exception ignored) {

            }
        }

        if (toggleable) {
            String label = NotificationRea1.moduleLabel(this.name);
            NotificationRea1.post(state ? NotificationRea1.Level.SUCCESS : NotificationRea1.Level.ERROR,
                    label + Lang.tr(state ? "notif.toggle.on" : "notif.toggle.off"));
        }
    }

    private void playClickSound() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null || minecraft.player == null) return;
            minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.35F, state ? 0.85F : 0.6F);
        } catch (Exception ignored) {

        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return state;
    }

    public void setEnabled(boolean enabled) {
        setState(enabled);
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public float getToggleAnimation() {
        return toggleAnimation;
    }
}
