package com.bedwars.shop;

public enum ShopType {
    SHOP,
    UPGRADE;

    public static ShopType fromInput(String input) {
        if (input == null) return null;
        if (input.equalsIgnoreCase("shop")) return SHOP;
        if (input.equalsIgnoreCase("upgrade")) return UPGRADE;
        return null;
    }
}
