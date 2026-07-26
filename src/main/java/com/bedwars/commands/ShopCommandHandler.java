package com.bedwars.commands;

import com.bedwars.BedwarsPlugin;
import com.bedwars.shop.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Gère toute la sous-arborescence "/bd shop <shop|upgrade> ...".
 * Séparée de BedwarsCommand pour ne pas rendre cette dernière illisible.
 */
public class ShopCommandHandler {

    private static final String PREFIX = ChatColor.GOLD + "[Bedwars] " + ChatColor.RESET;

    private final BedwarsPlugin plugin;

    public ShopCommandHandler(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    /** args[0] == "shop" */
    public void handle(Player player, String[] args) {
        if (args.length < 3) {
            sendUsage(player);
            return;
        }
        ShopType type = ShopType.fromInput(args[1]);
        if (type == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Le type doit être 'shop' ou 'upgrade'.");
            return;
        }
        String action = args[2].toLowerCase();

        switch (action) {
            case "display" -> handleDisplay(player, type);
            case "custom" -> handleCustom(player, type, args);
            case "addgui" -> handleAddGui(player, type, args);
            case "delete" -> handleDeleteGui(player, type, args);
            case "item" -> handleItem(player, type, args);
            default -> player.sendMessage(PREFIX + ChatColor.RED + "Action shop inconnue: " + action);
        }
    }

    private void handleDisplay(Player player, ShopType type) {
        plugin.getShopGUIManager().open(player, type, GuiDefinition.BASE, true);
    }

    // /bd shop shop custom <guiName> slot <num> item <material|none> [price <num> <fer|or|emeraude>]
    // /bd shop shop custom <guiName> delete <slot>
    // /bd shop upgrade custom slot <num> item <material> <upgradeKey> price <num> diamand [level <num>]
    // /bd shop upgrade custom delete <slot>   (extension raisonnable, non explicitement décrite)
    private void handleCustom(Player player, ShopType type, String[] args) {
        if (type == ShopType.SHOP) {
            handleCustomShop(player, args);
        } else {
            handleCustomUpgrade(player, args);
        }
    }

    private void handleCustomShop(Player player, String[] args) {
        // args: shop shop custom <guiName> ...
        if (args.length < 5) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd shop shop custom <gui> slot <n> item <item|none> [price <n> <fer|or|emeraude>]");
            return;
        }
        String guiName = args[3].toLowerCase();
        ShopConfig config = plugin.getShopConfigManager().getConfig();
        if (!config.shopGuiExists(guiName)) {
            player.sendMessage(PREFIX + ChatColor.RED + "Le gui '" + guiName + "' n'existe pas. Crée-le avec addgui d'abord.");
            return;
        }
        GuiDefinition gui = config.getOrCreateShopGui(guiName);

        if (args[4].equalsIgnoreCase("delete")) {
            if (args.length < 6) {
                player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd shop shop custom " + guiName + " delete <slot>");
                return;
            }
            int slot = parseSlot(player, args[5]);
            if (slot < 0) return;
            gui.setSlot(slot, GuiSlot.empty());
            plugin.getShopConfigManager().save();
            player.sendMessage(PREFIX + ChatColor.GREEN + "Emplacement " + slot + " vidé dans " + guiName + ".");
            return;
        }

        if (!args[4].equalsIgnoreCase("slot") || args.length < 7 || !args[6].equalsIgnoreCase("item")) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd shop shop custom " + guiName + " slot <n> item <item|none> [price <n> <fer|or|emeraude>]");
            return;
        }
        int slot = parseSlot(player, args[5]);
        if (slot < 0) return;
        if (args.length < 8) {
            player.sendMessage(PREFIX + ChatColor.RED + "Précise un item (ou 'none').");
            return;
        }
        String itemName = args[7];
        if (itemName.equalsIgnoreCase("none")) {
            gui.setSlot(slot, GuiSlot.empty());
            plugin.getShopConfigManager().save();
            player.sendMessage(PREFIX + ChatColor.GREEN + "Emplacement " + slot + " vidé dans " + guiName + ".");
            return;
        }
        Material material = Material.matchMaterial(itemName.toUpperCase());
        if (material == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Item Minecraft inconnu: " + itemName);
            return;
        }
        if (args.length < 11 || !args[8].equalsIgnoreCase("price")) {
            player.sendMessage(PREFIX + ChatColor.RED + "Il manque le prix: ... price <n> <fer|or|emeraude>");
            return;
        }
        int price;
        try {
            price = Integer.parseInt(args[9]);
        } catch (NumberFormatException e) {
            player.sendMessage(PREFIX + ChatColor.RED + "Le prix doit être un nombre.");
            return;
        }
        var currency = com.bedwars.arena.GeneratorType.fromInput(args[10]);
        if (currency == null || currency == com.bedwars.arena.GeneratorType.DIAMOND) {
            player.sendMessage(PREFIX + ChatColor.RED + "La monnaie doit être fer, or ou emeraude.");
            return;
        }
        gui.setSlot(slot, GuiSlot.shopItem(material, price, currency));
        plugin.getShopConfigManager().save();
        player.sendMessage(PREFIX + ChatColor.GREEN + "Emplacement " + slot + " de " + guiName + " configuré: "
                + material.name() + " pour " + price + " " + args[10] + ".");
    }

    private void handleCustomUpgrade(Player player, String[] args) {
        // args: shop upgrade custom ...
        if (args.length >= 5 && args[3].equalsIgnoreCase("delete")) {
            int slot = parseSlot(player, args[4]);
            if (slot < 0) return;
            plugin.getShopConfigManager().getConfig().getUpgradeGui().setSlot(slot, GuiSlot.empty());
            plugin.getShopConfigManager().save();
            player.sendMessage(PREFIX + ChatColor.GREEN + "Emplacement " + slot + " vidé dans le gui upgrade.");
            return;
        }
        // shop upgrade custom slot <n> item <material> <upgradeKey> price <n> diamand [level <n>]
        if (args.length < 11 || !args[3].equalsIgnoreCase("slot") || !args[5].equalsIgnoreCase("item")
                || !args[8].equalsIgnoreCase("price")) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd shop upgrade custom slot <n> item <item> <type> price <n> diamand [level <n>]");
            return;
        }
        int slot = parseSlot(player, args[4]);
        if (slot < 0) return;
        Material material = Material.matchMaterial(args[6].toUpperCase());
        if (material == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Item Minecraft inconnu: " + args[6]);
            return;
        }
        UpgradeType upgradeType = UpgradeType.fromCommandKey(args[7]);
        if (upgradeType == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Type d'amélioration inconnu: " + args[7]
                    + " (Heal, Sharp, Armur, maniac, chute, Forge, TrapA, TrapC, TrapM, Dragon)");
            return;
        }
        int startLevel = 1;
        if (args.length >= 13 && args[11].equalsIgnoreCase("level")) {
            try {
                startLevel = Integer.parseInt(args[12]);
            } catch (NumberFormatException ignored) {}
        }
        plugin.getShopConfigManager().getConfig().getUpgradeGui().setSlot(slot,
                GuiSlot.upgradeItem(material, upgradeType, startLevel));
        plugin.getShopConfigManager().save();
        player.sendMessage(PREFIX + ChatColor.GREEN + "Emplacement " + slot + " configuré: "
                + upgradeType.getLabel() + ".");
    }

    // /bd shop shop addgui slot <n> item <material> <guiName>
    private void handleAddGui(Player player, ShopType type, String[] args) {
        if (type != ShopType.SHOP) {
            player.sendMessage(PREFIX + ChatColor.RED + "addgui n'est disponible que pour le shop.");
            return;
        }
        if (args.length < 8 || !args[3].equalsIgnoreCase("slot") || !args[5].equalsIgnoreCase("item")) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd shop shop addgui slot <n> item <material> <nomDuGui>");
            return;
        }
        int slot = parseSlot(player, args[4]);
        if (slot < 0) return;
        Material material = Material.matchMaterial(args[6].toUpperCase());
        if (material == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Item Minecraft inconnu: " + args[6]);
            return;
        }
        String newGuiName = args[7];
        ShopConfig config = plugin.getShopConfigManager().getConfig();
        config.getOrCreateShopGui(newGuiName);
        config.getOrCreateShopGui(GuiDefinition.BASE).setSlot(slot, GuiSlot.shopLink(material, newGuiName.toLowerCase()));
        plugin.getShopConfigManager().save();
        player.sendMessage(PREFIX + ChatColor.GREEN + "Nouveau gui '" + newGuiName + "' créé et lié à l'emplacement " + slot + " du gui de base.");
    }

    // /bd shop shop delete <guiName>
    private void handleDeleteGui(Player player, ShopType type, String[] args) {
        if (type != ShopType.SHOP) {
            player.sendMessage(PREFIX + ChatColor.RED + "La suppression de gui n'est disponible que pour le shop.");
            return;
        }
        if (args.length < 4) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd shop shop delete <nomDuGui>");
            return;
        }
        String guiName = args[3];
        if (guiName.equalsIgnoreCase(GuiDefinition.BASE)) {
            player.sendMessage(PREFIX + ChatColor.RED + "Impossible de supprimer le gui de base.");
            return;
        }
        plugin.getShopConfigManager().getConfig().deleteShopGui(guiName);
        plugin.getShopConfigManager().save();
        player.sendMessage(PREFIX + ChatColor.GREEN + "Gui '" + guiName + "' supprimé.");
    }

    // /bd shop upgrade item <type> addlevel <n>
    // /bd shop upgrade item <type> levelprice <level> <price>
    private void handleItem(Player player, ShopType type, String[] args) {
        if (type != ShopType.UPGRADE) {
            player.sendMessage(PREFIX + ChatColor.RED + "'item' n'est disponible que pour l'upgrade.");
            return;
        }
        if (args.length < 6) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd shop upgrade item <type> <addlevel <n>|levelprice <lvl> <prix>>");
            return;
        }
        UpgradeType upgradeType = UpgradeType.fromCommandKey(args[3]);
        if (upgradeType == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Type d'amélioration inconnu: " + args[3]);
            return;
        }
        UpgradeTypeConfig cfg = plugin.getShopConfigManager().getConfig().getUpgradeConfig(upgradeType);

        if (args[4].equalsIgnoreCase("addlevel")) {
            if (!upgradeType.isLeveled()) {
                player.sendMessage(PREFIX + ChatColor.RED + upgradeType.getLabel() + " n'a pas de palier (achat unique).");
                return;
            }
            try {
                int levels = Integer.parseInt(args[5]);
                cfg.setMaxLevel(levels);
                plugin.getShopConfigManager().save();
                player.sendMessage(PREFIX + ChatColor.GREEN + "Nombre de niveaux de " + upgradeType.getLabel()
                        + " défini à " + cfg.getMaxLevel() + " (max autorisé: " + upgradeType.getDefaultMaxLevel() + ").");
            } catch (NumberFormatException e) {
                player.sendMessage(PREFIX + ChatColor.RED + "Le nombre de niveaux doit être un entier.");
            }
        } else if (args[4].equalsIgnoreCase("levelprice")) {
            if (args.length < 7) {
                player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd shop upgrade item " + args[3] + " levelprice <niveau> <prix>");
                return;
            }
            try {
                int level = Integer.parseInt(args[5]);
                int price = Integer.parseInt(args[6]);
                cfg.setPriceForLevel(level, price);
                plugin.getShopConfigManager().save();
                player.sendMessage(PREFIX + ChatColor.GREEN + "Prix du niveau " + level + " de " + upgradeType.getLabel()
                        + " défini à " + price + " diamants.");
            } catch (NumberFormatException e) {
                player.sendMessage(PREFIX + ChatColor.RED + "Le niveau et le prix doivent être des entiers.");
            }
        } else {
            player.sendMessage(PREFIX + ChatColor.RED + "Sous-action inconnue: " + args[4]);
        }
    }

    private int parseSlot(Player player, String raw) {
        try {
            int slot = Integer.parseInt(raw);
            if (slot < 0 || slot > 53) {
                player.sendMessage(PREFIX + ChatColor.RED + "Le numéro d'emplacement doit être entre 0 et 53.");
                return -1;
            }
            return slot;
        } catch (NumberFormatException e) {
            player.sendMessage(PREFIX + ChatColor.RED + "Le numéro d'emplacement doit être un nombre.");
            return -1;
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(PREFIX + ChatColor.YELLOW + "Utilisation:");
        player.sendMessage(ChatColor.GRAY + "/bd shop <shop|upgrade> display");
        player.sendMessage(ChatColor.GRAY + "/bd shop shop custom <gui> slot <n> item <item|none> [price <n> <fer|or|emeraude>]");
        player.sendMessage(ChatColor.GRAY + "/bd shop shop custom <gui> delete <slot>");
        player.sendMessage(ChatColor.GRAY + "/bd shop shop addgui slot <n> item <material> <nomDuGui>");
        player.sendMessage(ChatColor.GRAY + "/bd shop shop delete <nomDuGui>");
        player.sendMessage(ChatColor.GRAY + "/bd shop upgrade custom slot <n> item <material> <type> price <n> diamand [level <n>]");
        player.sendMessage(ChatColor.GRAY + "/bd shop upgrade item <type> addlevel <n>");
        player.sendMessage(ChatColor.GRAY + "/bd shop upgrade item <type> levelprice <niveau> <prix>");
    }
}
