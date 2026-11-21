package lol.silencecute.client.screen.fatality;

interface FaKeySink {
    boolean keyPressed(int key, int modifiers);

    default boolean mousePressed(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean charTyped(char character) {
        return false;
    }

    default void focusLost() {
    }
}
