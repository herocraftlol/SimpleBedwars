package com.bedwars.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Petites méthodes partagées entre le shop et les améliorations d'équipe pour
 * compter/retirer une monnaie (fer, or, émeraude, diamant) de l'inventaire d'un joueur.
 */
public final class EconomyUtil {

    private EconomyUtil() {}

    public static int countCurrency(Player player, Material currency) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == currency) total += stack.getAmount();
        }
        return total;
    }

    public static boolean hasCurrency(Player player, Material currency, int amount) {
        return countCurrency(player, currency) >= amount;
    }

    public static void removeCurrency(Player player, Material currency, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != currency) continue;
            int take = Math.min(stack.getAmount(), remaining);
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
            if (stack.getAmount() <= 0) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    public static String currencyName(Material currency, int amount) {
        return switch (currency) {
            case IRON_INGOT -> "Fer";
            case GOLD_INGOT -> "Or";
            case EMERALD -> "Émeraude" + (amount > 1 ? "s" : "");
            case DIAMOND -> "Diamant" + (amount > 1 ? "s" : "");
            default -> currency.name();
        };
    }
}
