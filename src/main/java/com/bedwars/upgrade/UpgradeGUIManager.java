package com.bedwars.upgrade;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.TeamColor;
import com.bedwars.game.GameInstance;
import com.bedwars.util.EconomyUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit et gère l'interface du PNJ "Amélioration" (Team Upgrades côté Hypixel) :
 * Forge, Sharpened Blades, Reinforced Armor, Maniac Miner, Heal Pool, 3 pièges,
 * Dragon Buff. Tout se paie en diamants, sauf les pièges qui se paient en émeraudes.
 */
public class UpgradeGUIManager {

    private static final int SIZE = 27;
    private static final int SLOT_FORGE = 10;
    private static final int SLOT_SHARPENED = 12;
    private static final int SLOT_ARMOR = 14;
    private static final int SLOT_MANIAC = 16;
    private static final int SLOT_HEAL_POOL = 19;
    private static final int[] TRAP_SLOTS = {21, 22, 23};
    private static final int SLOT_DRAGON = 25;

    private final BedwarsPlugin plugin;

    public UpgradeGUIManager(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Arena arena, TeamColor team) {
        UpgradeHolder holder = new UpgradeHolder(arena.getName(), team);
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Améliorations " + ChatColor.RESET
                        + ChatColor.GRAY + "- " + team.getColoredName());
        holder.setInventory(inv);
        populate(player, arena, inv, holder);
        player.openInventory(inv);
    }

    private void populate(Player player, Arena arena, Inventory inv, UpgradeHolder holder) {
        inv.clear();
        ItemStack filler = pane(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);

        GameInstance instance = plugin.getGameManager().getInstance(arena);
        TeamUpgrades upgrades = instance.getTeamUpgrades(holder.getTeam());

        inv.setItem(4, pane(Material.NETHER_STAR, ChatColor.AQUA + "" + ChatColor.BOLD + "Améliorations d'équipe",
                List.of(ChatColor.GRAY + "Achetées avec les diamants et", ChatColor.GRAY + "émeraudes de votre équipe.")));

        inv.setItem(SLOT_FORGE, forgeItem(upgrades));
        inv.setItem(SLOT_SHARPENED, levelItem(Material.IRON_SWORD, "Sharpened Blades",
                "Augmente les dégâts de vos armes.", upgrades.getSharpenedBlades(), TeamUpgrades.MAX_LEVEL_SHARPENED_BLADES,
                sharpenedPrice(upgrades.getSharpenedBlades())));
        inv.setItem(SLOT_ARMOR, levelItem(Material.IRON_CHESTPLATE, "Reinforced Armor",
                "Augmente la résistance de votre équipe.", upgrades.getReinforcedArmor(), TeamUpgrades.MAX_LEVEL_REINFORCED_ARMOR,
                armorPrice(upgrades.getReinforcedArmor())));
        inv.setItem(SLOT_MANIAC, levelItem(Material.GOLDEN_PICKAXE, "Maniac Miner",
                "Donne la Hâte près de votre base.", upgrades.getManiacMiner(), TeamUpgrades.MAX_LEVEL_MANIAC_MINER,
                2 + upgrades.getManiacMiner() * 2));

        inv.setItem(SLOT_HEAL_POOL, healPoolItem(upgrades));
        inv.setItem(SLOT_DRAGON, dragonBuffItem(upgrades));

        for (int i = 0; i < TRAP_SLOTS.length; i++) {
            inv.setItem(TRAP_SLOTS[i], trapSlotItem(upgrades, i));
        }
    }

    private int sharpenedPrice(int currentLevel) {
        return switch (currentLevel) {
            case 0 -> 4;
            case 1 -> 8;
            case 2 -> 12;
            default -> 16;
        };
    }

    private int armorPrice(int currentLevel) {
        return switch (currentLevel) {
            case 0 -> 5;
            case 1 -> 10;
            case 2 -> 20;
            default -> 30;
        };
    }

    private int forgePrice(int currentLevel) {
        return switch (currentLevel) {
            case 0 -> 4;
            case 1 -> 8;
            case 2 -> 12;
            default -> 16;
        };
    }

    private ItemStack forgeItem(TeamUpgrades upgrades) {
        String[] names = {"Forge de base", "Forge en fer", "Forge dorée", "Forge en émeraude", "Forge en obsidienne"};
        int level = upgrades.getForge();
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Accélère la production de fer");
        lore.add(ChatColor.GRAY + "et d'or de votre générateur.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Niveau actuel: " + ChatColor.YELLOW + names[Math.min(level, names.length - 1)]);
        if (level >= TeamUpgrades.MAX_LEVEL_FORGE) {
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "NIVEAU MAXIMUM");
        } else {
            lore.add(ChatColor.GOLD + "Prix: " + ChatColor.WHITE + forgePrice(level) + " Diamants");
            lore.add(ChatColor.YELLOW + "Cliquez pour améliorer -> " + names[level + 1]);
        }
        return pane(Material.FURNACE, ChatColor.GOLD + "" + ChatColor.BOLD + "Forge", lore);
    }

    private ItemStack levelItem(Material icon, String name, String description, int level, int max, int price) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + description);
        lore.add("");
        lore.add(ChatColor.GRAY + "Niveau: " + ChatColor.YELLOW + level + ChatColor.GRAY + "/" + max);
        if (level >= max) {
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "NIVEAU MAXIMUM");
        } else {
            lore.add(ChatColor.GOLD + "Prix: " + ChatColor.WHITE + price + " Diamants");
            lore.add(ChatColor.YELLOW + "Cliquez pour améliorer !");
        }
        return pane(icon, ChatColor.AQUA + "" + ChatColor.BOLD + name, lore);
    }

    private ItemStack healPoolItem(TeamUpgrades upgrades) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Régénère la vie des coéquipiers");
        lore.add(ChatColor.GRAY + "proches de votre lit.");
        lore.add("");
        if (upgrades.isHealPool()) {
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ACHETÉ");
        } else {
            lore.add(ChatColor.GOLD + "Prix: " + ChatColor.WHITE + "3 Diamants");
            lore.add(ChatColor.YELLOW + "Cliquez pour acheter !");
        }
        return pane(Material.BEACON, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Heal Pool", lore);
    }

    private ItemStack dragonBuffItem(TeamUpgrades upgrades) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Fait apparaître un dragon");
        lore.add(ChatColor.GRAY + "qui garde votre base.");
        lore.add("");
        if (upgrades.isDragonBuff()) {
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "ACHETÉ");
        } else {
            lore.add(ChatColor.GOLD + "Prix: " + ChatColor.WHITE + "5 Diamants");
            lore.add(ChatColor.YELLOW + "Cliquez pour acheter !");
        }
        return pane(Material.DRAGON_HEAD, ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Dragon Buff", lore);
    }

    private ItemStack trapSlotItem(TeamUpgrades upgrades, int index) {
        List<TrapType> queued = new ArrayList<>(upgrades.getTraps());
        if (index < queued.size()) {
            TrapType trap = queued.get(index);
            List<String> lore = new ArrayList<>();
            for (String line : trap.getDescription()) lore.add(ChatColor.GRAY + line);
            lore.add("");
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "EN ATTENTE (piège n°" + (index + 1) + ")");
            return pane(trap.getIcon(), trap.getColoredName(), lore);
        }
        if (index == queued.size() && queued.size() < TeamUpgrades.MAX_TRAPS) {
            int price = 1 << queued.size(); // 1, 2, 4 émeraudes
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Emplacement de piège n°" + (index + 1));
            lore.add("");
            lore.add(ChatColor.GOLD + "Prix: " + ChatColor.WHITE + price + " Émeraude(s)");
            lore.add(ChatColor.YELLOW + "Cliquez pour choisir un piège !");
            return pane(Material.TRIPWIRE_HOOK, ChatColor.YELLOW + "Acheter un piège", lore);
        }
        return pane(Material.GRAY_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "Emplacement de piège verrouillé", List.of());
    }

    // -----------------------------------------------------------------
    // Clics
    // -----------------------------------------------------------------

    /** Retourne true si le clic a été traité ici (menu d'amélioration ou choix de piège). */
    public boolean handleClick(Player player, Arena arena, Inventory topInventory, int slot) {
        if (topInventory.getHolder() instanceof TrapPickHolder pickHolder) {
            handleTrapPick(player, arena, pickHolder, slot);
            return true;
        }
        if (!(topInventory.getHolder() instanceof UpgradeHolder holder)) return false;

        GameInstance instance = plugin.getGameManager().getInstance(arena);
        TeamUpgrades upgrades = instance.getTeamUpgrades(holder.getTeam());

        if (slot == SLOT_FORGE) {
            buyLevel(player, upgrades::upgradeForge, forgePrice(upgrades.getForge()), Material.DIAMOND, "Forge");
        } else if (slot == SLOT_SHARPENED) {
            buyLevel(player, upgrades::upgradeSharpenedBlades, sharpenedPrice(upgrades.getSharpenedBlades()), Material.DIAMOND, "Sharpened Blades");
        } else if (slot == SLOT_ARMOR) {
            buyLevel(player, upgrades::upgradeReinforcedArmor, armorPrice(upgrades.getReinforcedArmor()), Material.DIAMOND, "Reinforced Armor");
        } else if (slot == SLOT_MANIAC) {
            int price = 2 + upgrades.getManiacMiner() * 2;
            buyLevel(player, upgrades::upgradeManiacMiner, price, Material.DIAMOND, "Maniac Miner");
        } else if (slot == SLOT_HEAL_POOL) {
            buyOnce(player, upgrades::buyHealPool, 3, Material.DIAMOND, "Heal Pool");
        } else if (slot == SLOT_DRAGON) {
            if (buyOnce(player, upgrades::buyDragonBuff, 5, Material.DIAMOND, "Dragon Buff")) {
                instance.spawnDragonGuard(holder.getTeam());
            }
        } else {
            for (int i = 0; i < TRAP_SLOTS.length; i++) {
                if (TRAP_SLOTS[i] != slot) continue;
                int queuedCount = upgrades.getTraps().size();
                if (i == queuedCount && queuedCount < TeamUpgrades.MAX_TRAPS) {
                    openTrapPicker(player, arena, holder.getTeam());
                }
                return true;
            }
        }

        populate(player, arena, topInventory, holder);
        return true;
    }

    private void buyLevel(Player player, java.util.function.BooleanSupplier upgrade, int price, Material currency, String name) {
        if (!EconomyUtil.hasCurrency(player, currency, price)) {
            player.sendMessage(ChatColor.RED + "Il vous manque des " + EconomyUtil.currencyName(currency, price) + " pour améliorer " + name + ".");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        if (!upgrade.getAsBoolean()) {
            player.sendMessage(ChatColor.RED + name + " est déjà au niveau maximum.");
            return;
        }
        EconomyUtil.removeCurrency(player, currency, price);
        player.sendMessage(ChatColor.GREEN + name + " amélioré pour toute l'équipe !");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    private boolean buyOnce(Player player, java.util.function.BooleanSupplier purchase, int price, Material currency, String name) {
        if (!EconomyUtil.hasCurrency(player, currency, price)) {
            player.sendMessage(ChatColor.RED + "Il vous manque des " + EconomyUtil.currencyName(currency, price) + " pour acheter " + name + ".");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return false;
        }
        if (!purchase.getAsBoolean()) {
            player.sendMessage(ChatColor.RED + name + " a déjà été acheté par votre équipe.");
            return false;
        }
        EconomyUtil.removeCurrency(player, currency, price);
        player.sendMessage(ChatColor.GREEN + name + " acheté pour toute l'équipe !");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        return true;
    }

    private void openTrapPicker(Player player, Arena arena, TeamColor team) {
        TrapPickHolder holder = new TrapPickHolder(arena.getName(), team);
        Inventory inv = Bukkit.createInventory(holder, 9, ChatColor.YELLOW + "Choisissez un piège");
        holder.setInventory(inv);

        GameInstance instance = plugin.getGameManager().getInstance(arena);
        int price = 1 << instance.getTeamUpgrades(team).getTraps().size();

        int slot = 2;
        for (TrapType trap : TrapType.values()) {
            List<String> lore = new ArrayList<>();
            for (String line : trap.getDescription()) lore.add(ChatColor.GRAY + line);
            lore.add("");
            lore.add(ChatColor.GOLD + "Prix: " + ChatColor.WHITE + price + " Émeraude(s)");
            inv.setItem(slot, pane(trap.getIcon(), trap.getColoredName(), lore));
            holder.getSlotTraps().put(slot, trap);
            slot += 2;
        }
        player.openInventory(inv);
    }

    private void handleTrapPick(Player player, Arena arena, TrapPickHolder holder, int slot) {
        TrapType trap = holder.getSlotTraps().get(slot);
        if (trap == null) return;

        GameInstance instance = plugin.getGameManager().getInstance(arena);
        TeamUpgrades upgrades = instance.getTeamUpgrades(holder.getTeam());
        int price = 1 << upgrades.getTraps().size();

        if (!EconomyUtil.hasCurrency(player, Material.EMERALD, price)) {
            player.sendMessage(ChatColor.RED + "Il vous manque des émeraudes pour acheter ce piège.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        if (!upgrades.addTrap(trap)) {
            player.sendMessage(ChatColor.RED + "Tous les emplacements de piège sont occupés.");
            return;
        }
        EconomyUtil.removeCurrency(player, Material.EMERALD, price);
        player.sendMessage(ChatColor.GREEN + "Piège posé : " + trap.getColoredName());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        player.closeInventory();
    }

    private ItemStack pane(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static class UpgradeHolder implements InventoryHolder {
        private final String arenaName;
        private final TeamColor team;
        private Inventory inventory;

        public UpgradeHolder(String arenaName, TeamColor team) {
            this.arenaName = arenaName;
            this.team = team;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public String getArenaName() {
            return arenaName;
        }

        public TeamColor getTeam() {
            return team;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public static class TrapPickHolder implements InventoryHolder {
        private final String arenaName;
        private final TeamColor team;
        private final java.util.Map<Integer, TrapType> slotTraps = new java.util.HashMap<>();
        private Inventory inventory;

        public TrapPickHolder(String arenaName, TeamColor team) {
            this.arenaName = arenaName;
            this.team = team;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        public String getArenaName() {
            return arenaName;
        }

        public TeamColor getTeam() {
            return team;
        }

        public java.util.Map<Integer, TrapType> getSlotTraps() {
            return slotTraps;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
