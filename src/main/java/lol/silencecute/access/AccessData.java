package lol.silencecute.access;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import lol.silencecute.api.Operator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

public final class AccessData extends SavedData {

    public static final String DATA_NAME = "silencecute_access";

    final Map<UUID, Operator> operators = new HashMap<>();

    final Map<UUID, Boolean> canChoose = new HashMap<>();

    final Set<UUID> noCooldown = new HashSet<>();

    static AccessData load(CompoundTag tag) {
        AccessData data = new AccessData();

        CompoundTag operatorTag = tag.getCompound("Operators");
        for (String key : operatorTag.getAllKeys()) {
            Operator operator = Operator.byInput(operatorTag.getString(key));
            if (operator == null || operator == Operator.NONE) continue;
            UUID uuid = parseUuid(key);
            if (uuid != null) data.operators.put(uuid, operator);
        }

        CompoundTag chooseTag = tag.getCompound("CanChoose");
        for (String key : chooseTag.getAllKeys()) {
            UUID uuid = parseUuid(key);
            if (uuid != null) data.canChoose.put(uuid, chooseTag.getBoolean(key));
        }

        ListTag freeTag = tag.getList("NoCooldown", Tag.TAG_STRING);
        for (int i = 0; i < freeTag.size(); i++) {
            UUID uuid = parseUuid(freeTag.getString(i));
            if (uuid != null) data.noCooldown.add(uuid);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag operatorTag = new CompoundTag();
        this.operators.forEach((uuid, operator) -> operatorTag.putString(uuid.toString(), operator.name()));
        tag.put("Operators", operatorTag);

        CompoundTag chooseTag = new CompoundTag();
        this.canChoose.forEach((uuid, allowed) -> chooseTag.putBoolean(uuid.toString(), allowed));
        tag.put("CanChoose", chooseTag);

        ListTag freeTag = new ListTag();
        this.noCooldown.forEach(uuid -> freeTag.add(StringTag.valueOf(uuid.toString())));
        tag.put("NoCooldown", freeTag);
        return tag;
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
