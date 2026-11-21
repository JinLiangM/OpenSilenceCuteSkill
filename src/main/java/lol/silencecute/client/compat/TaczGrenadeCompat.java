package lol.silencecute.client.compat;

import lol.silencecute.manager.LunaGrenadeService;
import lol.silencecute.manager.LunaShockArrowService;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;

import java.util.function.Predicate;

import com.mojang.logging.LogUtils;

import net.minecraft.world.entity.LivingEntity;

import net.minecraftforge.common.MinecraftForge;

import net.minecraftforge.eventbus.api.Event;

import net.minecraftforge.eventbus.api.EventPriority;

import net.minecraftforge.fml.ModList;

public final class TaczGrenadeCompat {

    private static Predicate<LivingEntity> clientBlocker = player -> false;

    private TaczGrenadeCompat() {

    }

    public static void initialize() {

        if (!ModList.get().isLoaded("tacz")) {

            return;

        }

        register("com.tacz.guns.api.event.common.GunShootEvent");

        register("com.tacz.guns.api.event.common.GunFireEvent");

    }

    public static void setClientBlocker(Predicate<LivingEntity> blocker) {

        clientBlocker = blocker;

    }

    private static void register(String name) {

        try {

            Class<? extends Event> type = Class.forName(name).asSubclass(Event.class);

            Method shooter = type.getMethod("getShooter");

            registerListener(type, shooter);

        } catch (ReflectiveOperationException | LinkageError exception) {

            LogUtils.getLogger().warn("无法注册 TaCZ 手雷开火拦截：{}", name, exception);

        }

    }

    private static <T extends Event> void registerListener(Class<T> type, Method shooterMethod) {

        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, type, event -> {

            try {

                if (shooterMethod.invoke(event) instanceof LivingEntity shooter

                        && event.isCancelable()) {

                    boolean blocked = shooter.level().isClientSide()

                            ? clientBlocker.test(shooter)

                            : LunaGrenadeService.blocksGunfire(shooter.getUUID(),

                                    shooter.level().getGameTime())

                                    || LunaShockArrowService.blocksGunfire(shooter.getUUID());

                    if (blocked) {

                        event.setCanceled(true);

                    }

                }

            } catch (IllegalAccessException | InvocationTargetException exception) {

                LogUtils.getLogger().warn("TaCZ 手雷开火拦截读取失败", exception);

            }

        });

    }

}

