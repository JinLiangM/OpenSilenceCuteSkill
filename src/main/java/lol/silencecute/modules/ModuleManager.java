package lol.silencecute.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lol.silencecute.modules.impl.admin.AdminAccessModule;
import lol.silencecute.modules.impl.admin.AdminCooldownModule;
import lol.silencecute.modules.impl.misc.OperatorModule;
import lol.silencecute.modules.impl.render.ClickGuiModule;
import lol.silencecute.modules.impl.render.HudChatModule;
import lol.silencecute.modules.impl.render.HudHealthModule;
import lol.silencecute.modules.impl.render.HudPassiveModule;
import lol.silencecute.modules.impl.render.HudPermissionModule;
import lol.silencecute.modules.impl.render.HudSkillsModule;
import lol.silencecute.modules.impl.render.NotificationModule;
import lol.silencecute.modules.impl.render.OperatorRenderModule;
import lol.silencecute.modules.impl.render.RendererModule;

public final class ModuleManager {

    private static ModuleManager instance;

    private final List<Module> modules = new ArrayList<>();
    private final Map<Class<? extends Module>, Module> classMap = new HashMap<>();
    private final Map<String, Module> nameMap = new HashMap<>();

    private ModuleManager() {
        initModules();
    }

    public static ModuleManager get() {
        if (instance == null) {
            instance = new ModuleManager();
        }
        return instance;
    }

    private void initModules() {
        registerModule(

                new AdminCooldownModule(),

                new AdminAccessModule(),
                new OperatorModule(),

                new OperatorRenderModule(),
                new RendererModule(),
                new ClickGuiModule(),
                new HudSkillsModule(),
                new HudPassiveModule(),
                new HudPermissionModule(),
                new HudHealthModule(),
                new HudChatModule(),
                new NotificationModule()
        );
    }

    private void registerModule(Module... modules) {
        for (Module module : modules) {
            registerModule(module);
        }
    }

    private void registerModule(Module module) {
        if (module == null) return;
        module.initModule();
        this.modules.add(module);
        this.classMap.put(module.getClass(), module);
        if (module.getName() != null) {
            this.nameMap.put(module.getName().toLowerCase(Locale.ROOT), module);
        }
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(this.modules);
    }

    public List<Module> getModulesByCategory(Category category) {
        List<Module> list = new ArrayList<>();
        for (Module module : this.modules) {
            if (module.getCategory() == category) list.add(module);
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        return (T) this.classMap.get(clazz);
    }

    public Module getModule(String name) {
        if (name == null) return null;
        return this.nameMap.get(name.toLowerCase(Locale.ROOT));
    }

    public void enforceAll() {
        for (Module module : this.modules) {
            if (module instanceof OperatorModule operatorModule) {
                operatorModule.enforceAlwaysOn();
                continue;
            }
            if (module.toggleable) continue;
            if (!module.state) module.state = true;
        }
    }

    public void onKeyPressed(int key) {
        if (key <= 0) return;
        for (Module module : this.modules) {
            if (module.getKey() == key) module.toggle();
        }
    }
}
