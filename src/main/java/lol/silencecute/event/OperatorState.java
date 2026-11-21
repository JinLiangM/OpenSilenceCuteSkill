package lol.silencecute.event;



import java.util.HashMap;

import java.util.Map;

import java.util.UUID;



import lol.silencecute.api.Operator;

import net.minecraftforge.event.entity.player.PlayerEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;





public final class OperatorState {

    private static final Map<UUID, Operator> CHOICES = new HashMap<>();



    private OperatorState() {

    }



    public static void set(UUID playerUuid, Operator operator) {

        CHOICES.put(playerUuid, operator);

    }



    public static Operator get(UUID playerUuid) {

        return CHOICES.getOrDefault(playerUuid, Operator.NONE);

    }



    public static boolean isWeilong(UUID playerUuid) {

        return get(playerUuid) == Operator.WEILONG;

    }



    public static boolean isLuna(UUID playerUuid) {

        return get(playerUuid) == Operator.LUNA;

    }



    public static boolean isJifeng(UUID playerUuid) {

        return get(playerUuid) == Operator.JIFENG;

    }



    @SubscribeEvent

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {

        CHOICES.remove(event.getEntity().getUUID());

    }

}


