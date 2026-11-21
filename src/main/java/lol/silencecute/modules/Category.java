package lol.silencecute.modules;

import java.util.Locale;

import lol.silencecute.Client;
import lol.silencecute.client.render.ui.Ripple;
import net.minecraft.resources.ResourceLocation;

public enum Category {

    RENDER("Render"),
    MISC("Misc"),
    ADMIN("Admin");

    public final String displayName;

    private final String id;

    public float selectedAnimation;

    public final Ripple ripple = new Ripple();

    Category(String displayName) {
        this.displayName = displayName;
        this.id = name().toLowerCase(Locale.ROOT);
    }

    public ResourceLocation icon() {
        return ResourceLocation.fromNamespaceAndPath(Client.MOD_ID,
                "textures/gui/clickgui/" + id + ".png");
    }

    public String key() {
        return "category." + id;
    }
}
