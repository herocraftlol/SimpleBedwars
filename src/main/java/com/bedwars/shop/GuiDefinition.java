package com.bedwars.shop;

import java.util.HashMap;
import java.util.Map;

public class GuiDefinition {

    public static final int SIZE = 54;
    public static final String BASE = "base";

    private final String name;
    private final Map<Integer, GuiSlot> slots = new HashMap<>();

    public GuiDefinition(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public GuiSlot getSlot(int index) {
        return slots.getOrDefault(index, GuiSlot.empty());
    }

    public void setSlot(int index, GuiSlot slot) {
        if (index < 0 || index >= SIZE) return;
        if (slot == null || slot.getKind() == GuiSlot.Kind.EMPTY) {
            slots.remove(index);
        } else {
            slots.put(index, slot);
        }
    }

    public Map<Integer, GuiSlot> getSlots() {
        return slots;
    }
}
