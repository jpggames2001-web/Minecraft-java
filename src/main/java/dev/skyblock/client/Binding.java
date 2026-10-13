package dev.skyblock.client;
import static org.lwjgl.glfw.GLFW.*;
public enum Binding {
    FORWARD("Forward",GLFW_KEY_W), BACK("Back",GLFW_KEY_S), LEFT("Left",GLFW_KEY_A), RIGHT("Right",GLFW_KEY_D),
    JUMP("Jump",GLFW_KEY_SPACE), SNEAK("Sneak",GLFW_KEY_LEFT_SHIFT), SPRINT("Sprint",GLFW_KEY_LEFT_CONTROL),
    ATTACK("Attack",-100), USE("Use item",-101), PICK("Pick block",-102), INVENTORY("Inventory",GLFW_KEY_E), DROP("Drop",GLFW_KEY_Q),
    CHAT("Chat",GLFW_KEY_T), PLAYER_LIST("Player list",GLFW_KEY_TAB), RECIPE("Recipe guide",GLFW_KEY_R), SCREENSHOT("Screenshot",GLFW_KEY_F2), DEBUG("Debug",GLFW_KEY_F3),
    SLOT1("Hotbar 1",GLFW_KEY_1),SLOT2("Hotbar 2",GLFW_KEY_2),SLOT3("Hotbar 3",GLFW_KEY_3),SLOT4("Hotbar 4",GLFW_KEY_4),SLOT5("Hotbar 5",GLFW_KEY_5),SLOT6("Hotbar 6",GLFW_KEY_6),SLOT7("Hotbar 7",GLFW_KEY_7),SLOT8("Hotbar 8",GLFW_KEY_8),SLOT9("Hotbar 9",GLFW_KEY_9);
    public final String label; public final int defaultCode;
    Binding(String label,int code) { this.label=label; defaultCode=code; }
    public static String keyName(int code) {
        if(code==0) return "Unbound"; if(code<=-100) return "Mouse "+(-code-99);
        if(code>=33 && code<=96) return ""+(char)code;
        return switch(code) { case GLFW_KEY_SPACE -> "Space"; case GLFW_KEY_LEFT_SHIFT -> "Left Shift"; case GLFW_KEY_RIGHT_SHIFT -> "Right Shift"; case GLFW_KEY_LEFT_CONTROL -> "Left Ctrl"; case GLFW_KEY_RIGHT_CONTROL -> "Right Ctrl"; case GLFW_KEY_TAB -> "Tab"; case GLFW_KEY_UP -> "Up"; case GLFW_KEY_DOWN -> "Down"; case GLFW_KEY_LEFT -> "Left"; case GLFW_KEY_RIGHT -> "Right"; case GLFW_KEY_DELETE -> "Delete"; case GLFW_KEY_INSERT -> "Insert"; case GLFW_KEY_HOME -> "Home"; case GLFW_KEY_END -> "End"; case GLFW_KEY_PAGE_UP -> "Page Up"; case GLFW_KEY_PAGE_DOWN -> "Page Down"; case GLFW_KEY_LEFT_ALT -> "Left Alt"; case GLFW_KEY_RIGHT_ALT -> "Right Alt"; case GLFW_KEY_ENTER -> "Enter"; case GLFW_KEY_BACKSPACE -> "Backspace"; default -> code>=GLFW_KEY_F1 && code<=GLFW_KEY_F25?"F"+(code-GLFW_KEY_F1+1):"Key "+code; };
    }
}
