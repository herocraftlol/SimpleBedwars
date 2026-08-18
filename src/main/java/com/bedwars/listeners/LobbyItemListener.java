package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.TeamColor;
import com.bedwars.game.GameInstance;
import com.bedwars.util.LobbyItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;

/**
 * Gère les 3 items spéciaux donnés dans le lobby d'attente :
 *  - le diamant "Forcer le lancement" (admins uniquement) ;
 *  - le bloc "Choisir son équipe" (ouvre un menu de sélection) ;
 *  - le bloc barrière "Quitter la partie".
 */
public class LobbyItemListener implements Listener {

    private final BedwarsPlugin plugin;

    public LobbyItemListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!LobbyItemUtil.isAnyLobbyItem(item)) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        GameInstance instance = plugin.getGameManager().findInstanceOf(player);
        if (instance == null) return;

        if (LobbyItemUtil.isForceStart(item)) {
            if (!player.hasPermission("bedwars.admin")) return;
            instance.forceStart(player);
        } else if (LobbyItemUtil.isTeamSelect(item)) {
            openTeamSelect(player, instance);
        } else if (LobbyItemUtil.isLeave(item)) {
            plugin.getGameManager().leave(player);
            player.sendMessage(ChatColor.GOLD + "[Bedwars] " + ChatColor.RESET + "Vous avez quitté la partie.");
        }
    }

    private void openTeamSelect(Player player, GameInstance instance) {
        Arena arena = instance.getArena();
        TeamColor[] colors = TeamColor.forTeamCount(arena.getTeamCount());
        int size = ((colors.length - 1) / 9 + 1) * 9;
        size = Math.max(9, Math.min(54, size));

        TeamSelectHolder holder = new TeamSelectHolder(arena.getName());
        Inventory inv = Bukkit.createInventory(holder, size, ChatColor.GREEN + "" + ChatColor.BOLD + "Choisir son équipe");
        holder.setInventory(inv);

        int perTeam = Math.max(1, arena.getPlayersPerTeam());
        for (int i = 0; i < colors.length && i < size; i++) {
            TeamColor color = colors[i];
            int current = instance.countPreferred(color);
            boolean full = current >= perTeam;
            boolean isMine = color == instance.getPreferredTeam(player.getUniqueId());

            Material wool = Material.matchMaterial(color.getDyeColor().name() + "_WOOL");
            if (wool == null) wool = Material.WHITE_WOOL;

            ItemStack item = new ItemStack(wool);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((isMine ? ChatColor.YELLOW + "" + ChatColor.BOLD : ChatColor.GREEN + "") + color.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Joueurs: " + ChatColor.WHITE + current + ChatColor.GRAY + "/" + perTeam);
            lore.add("");
            if (isMine) {
                lore.add(ChatColor.YELLOW + "» Équipe choisie «");
            } else if (full) {
                lore.add(ChatColor.RED + "Équipe complète");
            } else {
                lore.add(ChatColor.YELLOW + "Cliquez pour choisir cette équipe !");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.setItem(i, item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TeamSelectHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        Arena arena = plugin.getArenaManager().getArena(holder.getArenaName());
        if (arena == null) return;
        GameInstance instance = plugin.getGameManager().findInstanceOf(player);
        if (instance == null) return;

        TeamColor[] colors = TeamColor.forTeamCount(arena.getTeamCount());
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= colors.length) return;
        TeamColor color = colors[slot];

        int perTeam = Math.max(1, arena.getPlayersPerTeam());
        boolean alreadyMine = color == instance.getPreferredTeam(player.getUniqueId());
        if (!alreadyMine && instance.countPreferred(color) >= perTeam) {
            player.sendMessage(ChatColor.RED + "Cette équipe est déjà complète.");
            return;
        }

        instance.setPreferredTeam(player.getUniqueId(), color);
        player.getInventory().setItem(LobbyItemUtil.SLOT_TEAM_SELECT, LobbyItemUtil.createTeamSelectItem(color));
        player.sendMessage(ChatColor.GREEN + "Vous avez choisi l'équipe " + color.getColoredName() + ChatColor.GREEN + " !");
        player.closeInventory();
    }

    public static class TeamSelectHolder implements InventoryHolder {
        private final String arenaName;
        private Inventory inventory;

        public TeamSelectHolder(String arenaName) {
            this.arenaName = arenaName;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public String getArenaName() {
            return arenaName;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
