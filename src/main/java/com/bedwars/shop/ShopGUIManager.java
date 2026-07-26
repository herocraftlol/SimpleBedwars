package com.bedwars.shop;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.ArenaTeam;
import com.bedwars.arena.GeneratorType;
import com.bedwars.arena.TeamColor;
import com.bedwars.game.GameInstance;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopGUIManager {

    private final BedwarsPlugin plugin;

    public ShopGUIManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, ShopType type, String guiName, boolean preview) {
        GuiDefinition def = resolveGui(type, guiName);
        if (def == null) {
            player.sendMessage(ChatColor.RED + "Ce GUI n'existe pas: " + guiName);
            return;
        }
        Inventory inv = build(type, def, preview);
        player.openInventory(inv);
    }

    private GuiDefinition resolveGui(ShopType type, String guiName) {
        ShopConfig config = plugin.getShopConfigManager().getConfig();
        if (type == ShopType.UPGRADE) return config.getUpgradeGui();
        return config.getShopGui(guiName == null ? GuiDefinition.BASE : guiName);
    }

    private Inventory build(ShopType type, GuiDefinition def, boolean preview) {
        String title = ChatColor.DARK_GRAY + "" + ChatColor.BOLD
                + (type == ShopType.SHOP ? "Shop" : "Amélioration")
                + ChatColor.RESET + ChatColor.DARK_GRAY + " » " + def.getName()
                + (preview ? ChatColor.YELLOW + " (aperçu)" : "");
        ShopGUIHolder holder = new ShopGUIHolder(type, def.getName(), preview);
        Inventory inv = plugin.getServer().createInventory(holder, GuiDefinition.SIZE, title);

        ItemStack emptyPane = pane(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Vide", List.of());
        for (int i = 0; i < GuiDefinition.SIZE; i++) {
            inv.setItem(i, emptyPane);
        }

        for (int i = 0; i < GuiDefinition.SIZE; i++) {
            GuiSlot slot = def.getSlot(i);
            ItemStack item = renderSlot(slot);
            if (item != null) inv.setItem(i, item);
        }

        if (!def.getName().equalsIgnoreCase(GuiDefinition.BASE) && type == ShopType.SHOP) {
            inv.setItem(49, pane(Material.ARROW, ChatColor.YELLOW + "« Retour", List.of()));
        }

        return inv;
    }

    private ItemStack renderSlot(GuiSlot slot) {
        switch (slot.getKind()) {
            case SHOP_ITEM -> {
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Prix: " + ChatColor.WHITE + slot.getPrice() + " "
                        + currencyLabel(slot.getCurrency()) + ChatColor.GRAY + " pour " + slot.getAmount());
                String name = ChatColor.AQUA + prettify(slot.getMaterial().name())
                        + (slot.getAmount() > 1 ? ChatColor.GRAY + " x" + slot.getAmount() : "");
                return item(slot.getMaterial(), name, lore);
            }
            case SHOP_LINK -> {
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Ouvre: " + ChatColor.WHITE + slot.getLinkedGui());
                return item(slot.getMaterial(), ChatColor.GOLD + slot.getLinkedGui(), lore);
            }
            case UPGRADE_ITEM -> {
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Améliore: " + ChatColor.WHITE + slot.getUpgradeType().getLabel());
                return item(slot.getMaterial(), ChatColor.GREEN + slot.getUpgradeType().getLabel(), lore);
            }
            default -> {
                return null;
            }
        }
    }

    private String currencyLabel(GeneratorType type) {
        return switch (type) {
            case FER -> "Fer";
            case OR -> "Or";
            case EMERAUDE -> "Émeraude";
            case DIAMOND -> "Diamant";
        };
    }

    private String prettify(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private ItemStack pane(Material material, String name, List<String> lore) {
        return item(material, name, lore);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // -----------------------------------------------------------------
    // Gestion des clics (appelée depuis ShopListener)
    // -----------------------------------------------------------------

    public void handleClick(Player player, ShopGUIHolder holder, int slotIndex) {
        if (holder.type() == ShopType.SHOP && !holder.guiName().equalsIgnoreCase(GuiDefinition.BASE) && slotIndex == 49) {
            open(player, ShopType.SHOP, GuiDefinition.BASE, holder.preview());
            return;
        }

        GuiDefinition def = resolveGui(holder.type(), holder.guiName());
        if (def == null) return;
        GuiSlot slot = def.getSlot(slotIndex);
        if (slot.getKind() == GuiSlot.Kind.EMPTY) return;

        if (holder.preview()) {
            if (slot.getKind() == GuiSlot.Kind.SHOP_LINK) {
                open(player, ShopType.SHOP, slot.getLinkedGui(), true);
            }
            return; // pas d'achat en mode aperçu
        }

        switch (slot.getKind()) {
            case SHOP_LINK -> open(player, ShopType.SHOP, slot.getLinkedGui(), false);
            case SHOP_ITEM -> purchaseShopItem(player, slot);
            case UPGRADE_ITEM -> purchaseUpgrade(player, slot.getUpgradeType());
            default -> {}
        }
    }

    private void purchaseShopItem(Player player, GuiSlot slot) {
        Material currencyMaterial = slot.getCurrency().getMaterial();
        int have = countItems(player, currencyMaterial);
        if (have < slot.getPrice()) {
            player.sendMessage(ChatColor.RED + "Il vous manque " + (slot.getPrice() - have) + " "
                    + currencyLabel(slot.getCurrency()) + " pour acheter cet objet.");
            return;
        }
        removeItems(player, currencyMaterial, slot.getPrice());
        Material material = resolveMaterialForBuyer(slot.getMaterial(), player);
        giveOrDrop(player, new ItemStack(material, slot.getAmount()));
        player.sendMessage(ChatColor.GREEN + "Achat effectué: " + prettify(material.name())
                + (slot.getAmount() > 1 ? " x" + slot.getAmount() : ""));
    }

    /**
     * Recolore automatiquement la laine (et uniquement la laine) à la couleur de
     * l'équipe de l'acheteur, comme sur Hypixel. Les autres blocs restent tels
     * quels (le shop étant partagé entre toutes les maps, il n'y a qu'une seule
     * couleur possible pour ceux-ci).
     */
    private Material resolveMaterialForBuyer(Material configured, Player buyer) {
        if (configured != Material.WHITE_WOOL) return configured;
        GameInstance game = plugin.getGameManager().findInstanceOf(buyer);
        if (game == null) return configured;
        TeamColor color = game.getTeamColor(buyer);
        if (color == null) return configured;
        Material woolMaterial = Material.matchMaterial(color.getDyeColor().name() + "_WOOL");
        return woolMaterial != null ? woolMaterial : configured;
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        var leftover = player.getInventory().addItem(stack);
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), extra);
        }
    }

    private void purchaseUpgrade(Player player, UpgradeType type) {
        GameInstance game = plugin.getGameManager().findInstanceOf(player);
        if (game == null) {
            player.sendMessage(ChatColor.RED + "Vous devez être dans une partie pour acheter une amélioration.");
            return;
        }
        TeamColor color = game.getTeamColor(player);
        ArenaTeam team = color != null ? game.getArena().getTeams().get(color) : null;
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Impossible de déterminer votre équipe.");
            return;
        }
        game.purchaseUpgrade(player, team, type);
    }

    private int countItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) count += item.getAmount();
        }
        return count;
    }

    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) continue;
            int take = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - take);
            remaining -= take;
            if (item.getAmount() <= 0) player.getInventory().setItem(i, null);
        }
    }

    public record ShopGUIHolder(ShopType type, String guiName, boolean preview) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
