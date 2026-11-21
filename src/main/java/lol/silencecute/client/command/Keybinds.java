package lol.silencecute.client.command;

import org.lwjgl.glfw.GLFW;

public final class Keybinds {

    private Keybinds() {
    }

    public static int parse(String name) {
        if (name == null || name.isEmpty()) return -1;
        String key = name.trim().toUpperCase(java.util.Locale.ROOT).replace("_", "").replace(" ", "");

        switch (key) {
            case "RSHIFT", "RIGHTSHIFT" -> {
                return GLFW.GLFW_KEY_RIGHT_SHIFT;
            }
            case "LSHIFT", "LEFTSHIFT" -> {
                return GLFW.GLFW_KEY_LEFT_SHIFT;
            }
            case "RCTRL", "RCONTROL", "RIGHTCTRL" -> {
                return GLFW.GLFW_KEY_RIGHT_CONTROL;
            }
            case "LCTRL", "LCONTROL", "LEFTCTRL" -> {
                return GLFW.GLFW_KEY_LEFT_CONTROL;
            }
            case "RALT", "RIGHTALT" -> {
                return GLFW.GLFW_KEY_RIGHT_ALT;
            }
            case "LALT", "LEFTALT" -> {
                return GLFW.GLFW_KEY_LEFT_ALT;
            }
            case "TAB" -> {
                return GLFW.GLFW_KEY_TAB;
            }
            case "SPACE" -> {
                return GLFW.GLFW_KEY_SPACE;
            }
            case "ENTER", "RETURN" -> {
                return GLFW.GLFW_KEY_ENTER;
            }
            case "ESC", "ESCAPE" -> {
                return GLFW.GLFW_KEY_ESCAPE;
            }
            case "GRAVE", "`", "TILDE", "~" -> {
                return GLFW.GLFW_KEY_GRAVE_ACCENT;
            }
            case "CAPS", "CAPSLOCK" -> {
                return GLFW.GLFW_KEY_CAPS_LOCK;
            }
            case "INSERT", "INS" -> {
                return GLFW.GLFW_KEY_INSERT;
            }
            case "DELETE", "DEL" -> {
                return GLFW.GLFW_KEY_DELETE;
            }
            case "HOME" -> {
                return GLFW.GLFW_KEY_HOME;
            }
            case "END" -> {
                return GLFW.GLFW_KEY_END;
            }
            case "PAGEUP", "PGUP" -> {
                return GLFW.GLFW_KEY_PAGE_UP;
            }
            case "PAGEDOWN", "PGDN" -> {
                return GLFW.GLFW_KEY_PAGE_DOWN;
            }
            case "UP" -> {
                return GLFW.GLFW_KEY_UP;
            }
            case "DOWN" -> {
                return GLFW.GLFW_KEY_DOWN;
            }
            case "LEFT" -> {
                return GLFW.GLFW_KEY_LEFT;
            }
            case "RIGHT" -> {
                return GLFW.GLFW_KEY_RIGHT;
            }
            default -> {

                if (key.length() >= 2 && key.charAt(0) == 'F') {
                    try {
                        int index = Integer.parseInt(key.substring(1));
                        if (index >= 1 && index <= 25) return GLFW.GLFW_KEY_F1 + (index - 1);
                    } catch (NumberFormatException ignored) {

                    }
                }

                if (key.length() == 1) {
                    char c = key.charAt(0);
                    if (c >= 'A' && c <= 'Z') return GLFW.GLFW_KEY_A + (c - 'A');
                    if (c >= '0' && c <= '9') return GLFW.GLFW_KEY_0 + (c - '0');
                }
            }
        }
        return -1;
    }

    public static int scanFromKey(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> 0x01;
            case GLFW.GLFW_KEY_TAB -> 0x0F;
            case GLFW.GLFW_KEY_LEFT_CONTROL -> 0x1D;
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> 0x11D;
            case GLFW.GLFW_KEY_LEFT_SHIFT -> 0x2A;
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> 0x136;
            case GLFW.GLFW_KEY_LEFT_ALT -> 0x38;
            case GLFW.GLFW_KEY_RIGHT_ALT -> 0x138;
            case GLFW.GLFW_KEY_CAPS_LOCK -> 0x3A;
            default -> -1;
        };
    }

    public static int keyFromScan(int scan) {
        if (scan <= 0) return -1;
        boolean extended = (scan & 0x100) != 0;
        return switch (scan & 0xFF) {
            case 0x01 -> GLFW.GLFW_KEY_ESCAPE;
            case 0x0F -> GLFW.GLFW_KEY_TAB;
            case 0x1D -> extended ? GLFW.GLFW_KEY_RIGHT_CONTROL : GLFW.GLFW_KEY_LEFT_CONTROL;
            case 0x2A -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case 0x36 -> GLFW.GLFW_KEY_RIGHT_SHIFT;
            case 0x38 -> extended ? GLFW.GLFW_KEY_RIGHT_ALT : GLFW.GLFW_KEY_LEFT_ALT;
            case 0x3A -> GLFW.GLFW_KEY_CAPS_LOCK;
            default -> -1;
        };
    }

    public static boolean sameScan(int a, int b) {
        if (a <= 0 || b <= 0) return false;
        if (a == b) return true;
        int lowA = a & 0xFF;
        int lowB = b & 0xFF;
        if (lowA != lowB) return false;

        if (lowA == 0x1D || lowA == 0x38) return (a & 0x100) == (b & 0x100);
        return true;
    }

    public static String name(int code) {
        if (code <= 0) return "未绑定";
        if (code >= GLFW.GLFW_KEY_A && code <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('A' + (code - GLFW.GLFW_KEY_A)));
        }
        if (code >= GLFW.GLFW_KEY_0 && code <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + (code - GLFW.GLFW_KEY_0)));
        }
        if (code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F25) {
            return "F" + (code - GLFW.GLFW_KEY_F1 + 1);
        }
        if (code == GLFW.GLFW_KEY_RIGHT_SHIFT) return "RSHIFT";
        if (code == GLFW.GLFW_KEY_LEFT_SHIFT) return "LSHIFT";
        if (code == GLFW.GLFW_KEY_RIGHT_CONTROL) return "RCONTROL";
        if (code == GLFW.GLFW_KEY_LEFT_CONTROL) return "LCONTROL";
        if (code == GLFW.GLFW_KEY_RIGHT_ALT) return "RALT";
        if (code == GLFW.GLFW_KEY_LEFT_ALT) return "LALT";
        if (code == GLFW.GLFW_KEY_TAB) return "TAB";
        if (code == GLFW.GLFW_KEY_GRAVE_ACCENT) return "GRAVE";
        return "Key" + code;
    }
}
