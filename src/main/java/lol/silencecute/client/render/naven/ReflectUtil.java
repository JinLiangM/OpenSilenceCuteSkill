package lol.silencecute.client.render.naven;

import java.lang.reflect.Field;

public final class ReflectUtil {

    private ReflectUtil() {
    }

    public static Field getField(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    public static Object getFieldValue(Class<?> clazz, String name, Object instance) {
        Field field = getField(clazz, name);
        if (field == null) return null;
        try {
            return field.get(instance);
        } catch (IllegalAccessException exception) {
            return null;
        }
    }

    public static Boolean setFieldValue(Class<?> clazz, String name, Object instance, Object value) {
        Field field = getField(clazz, name);
        if (field == null) return Boolean.FALSE;
        try {
            field.set(instance, value);
            return Boolean.TRUE;
        } catch (IllegalAccessException exception) {
            return Boolean.FALSE;
        }
    }
}
