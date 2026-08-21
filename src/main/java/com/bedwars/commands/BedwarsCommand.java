package com.bedwars.commands;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BedwarsCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = ChatColor.GOLD + "[Bedwars] " + ChatColor.RESET;

    private final BedwarsPlugin plugin;
    /** Suppressions en attente de confirmation (/bd delete <nom> puis /bd delete <nom> confirm). */
    private final java.util.Map<java.util.UUID, PendingDelete> pendingDeletes = new java.util.HashMap<>();

    private record PendingDelete(String arenaName, long expiresAt) {}

    public BedwarsCommand(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // --- Commandes globales, non liées à une arène précise ---
        if (sub.equals("create")) {
            return handleCreate(sender, args);
        }
        if (sub.equals("delete")) {
            return handleDelete(sender, args);
        }
        if (sub.equals("copy")) {
            return handleCopy(sender, args);
        }
        if (sub.equals("shop")) {
            return handleShopConfig(sender, args);
        }
        if (sub.equals("arene")) {
            return handleArene(sender, args);
        }
        if (sub.equals("join")) {
            return handleJoin(sender, args);
        }
        if (sub.equals("spectate")) {
            return handleSpectate(sender, args);
        }
        if (sub.equals("leave")) {
            return handleLeave(sender);
        }
        if (sub.equals("quickmenu")) {
            return handleQuickMenu(sender, args);
        }
        if (sub.equals("list")) {
            return handleList(sender);
        }

        // --- Commandes liées à une arène : /bd <nom> <action> ... ---
        Arena arena = plugin.getArenaManager().getArena(args[0]);
        if (arena == null) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Arène inconnue: " + args[0]);
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd " + arena.getName() + " <action>");
            return true;
        }

        if (!sender.hasPermission("bedwars.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Vous n'avez pas la permission de configurer les arènes.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Cette action doit être exécutée en jeu.");
            return true;
        }

        String action = args[1].toLowerCase();
        switch (action) {
            case "equipe" -> handleEquipe(player, arena, args);
            case "pos1" -> {
                arena.setGamePos1(player.getLocation());
                arena.setGameZoneConfirmed(false);
                player.sendMessage(PREFIX + "Position 1 de la zone de jeu définie pour " + arena.getName() + ".");
            }
            case "pos2" -> {
                arena.setGamePos2(player.getLocation());
                arena.setGameZoneConfirmed(false);
                player.sendMessage(PREFIX + "Position 2 de la zone de jeu définie pour " + arena.getName() + ".");
            }
            case "posconfirm" -> {
                if (arena.getGamePos1() == null || arena.getGamePos2() == null) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Il faut définir pos1 et pos2 avant de confirmer.");
                } else {
                    arena.setGameZoneConfirmed(true);
                    player.sendMessage(PREFIX + ChatColor.GREEN + "Zone de jeu confirmée pour " + arena.getName() + ".");
                }
            }
            case "bed" -> handleBed(player, arena, args);
            case "spawn" -> handleSpawn(player, arena, args);
            case "item" -> handleItem(player, arena, args);
            case "forge" -> handleForge(player, arena, args);
            case "chest" -> handleChest(player, arena, args);
            case "geninfo" -> handleGenInfo(player, arena);
            case "reset" -> handleReset(player, arena);
            case "shop" -> handleShop(player, arena, args);
            case "spec" -> {
                arena.setSpecLocation(player.getLocation());
                player.sendMessage(PREFIX + "Centre du lobby d'attente / spawn spectateurs défini pour " + arena.getName() + ".");
            }
            case "specspawn" -> {
                arena.setSpectatorSpawnLocation(player.getLocation());
                player.sendMessage(PREFIX + "Point de spawn des spectateurs (généralement le milieu de la map) défini pour "
                        + arena.getName() + ". Les joueurs éliminés définitivement y seront téléportés.");
            }
            case "minplayers" -> handleMinPlayers(player, arena, args);
            case "save" -> handleSave(player, arena);
            case "config", "edit" -> {
                arena.setSaved(false);
                arena.setState(ArenaState.SETUP);
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Mode édition activé pour " + arena.getName()
                        + " : vous pouvez à nouveau modifier librement la map (blocs, etc.)."
                        + " Refaites /bd " + arena.getName() + " save une fois vos modifications terminées.");
            }
            default -> player.sendMessage(PREFIX + ChatColor.RED + "Action inconnue: " + action);
        }

        plugin.getArenaManager().saveArena(arena);
        return true;
    }

    // -----------------------------------------------------------------
    // Sous-commandes globales
    // -----------------------------------------------------------------

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bedwars.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd create <nom>");
            return true;
        }
        String name = args[1];
        if (plugin.getArenaManager().getArena(name) != null) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Une arène nommée " + name + " existe déjà.");
            return true;
        }
        plugin.getArenaManager().createArena(name);
        sender.sendMessage(PREFIX + ChatColor.GREEN + "Arène " + name + " créée. Configurez-la avec /bd " + name + " ...");
        return true;
    }

    /**
     * /bd delete <nom> : supprime intégralement une arène (config, région sauvegardée, NPC,
     * partie en cours). Demande une confirmation explicite avant d'agir : /bd delete <nom> confirm
     * dans les <delete-confirmation-seconds> secondes.
     */
    private boolean handleDelete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bedwars.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd delete <nom>");
            return true;
        }
        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Arène inconnue: " + args[1]);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Cette action doit être exécutée en jeu.");
            return true;
        }

        boolean confirming = args.length >= 3 && args[2].equalsIgnoreCase("confirm");
        int delaySeconds = plugin.getConfig().getInt("game.delete-confirmation-seconds", 15);

        if (!confirming) {
            pendingDeletes.put(player.getUniqueId(),
                    new PendingDelete(arena.getName(), System.currentTimeMillis() + delaySeconds * 1000L));
            player.sendMessage(PREFIX + ChatColor.RED + "" + ChatColor.BOLD + "Attention: "
                    + ChatColor.RED + "cette action va supprimer DÉFINITIVEMENT la map " + ChatColor.YELLOW + arena.getName()
                    + ChatColor.RED + " (configuration, région sauvegardée, PNJ).");
            player.sendMessage(PREFIX + ChatColor.RED + "Tapez " + ChatColor.YELLOW + "/bd delete " + arena.getName() + " confirm"
                    + ChatColor.RED + " dans les " + delaySeconds + " secondes pour confirmer.");
            return true;
        }

        PendingDelete pending = pendingDeletes.remove(player.getUniqueId());
        if (pending == null || !pending.arenaName().equalsIgnoreCase(arena.getName())
                || System.currentTimeMillis() > pending.expiresAt()) {
            player.sendMessage(PREFIX + ChatColor.RED + "Aucune suppression en attente pour cette map (ou délai expiré). "
                    + "Retapez /bd delete " + arena.getName() + " pour recommencer.");
            return true;
        }

        deleteArena(arena);
        player.sendMessage(PREFIX + ChatColor.GREEN + "Map " + ChatColor.YELLOW + arena.getName()
                + ChatColor.GREEN + " supprimée définitivement.");
        return true;
    }

    /**
     * /bd shop <catégorie> <slot> <item> <quantité> <prix> <minerai>
     * Configure (ajoute ou remplace) un article du shop, sans avoir à toucher au code.
     * Le nom de catégorie "tools" est réservé à l'onglet spécial pioche/hache à paliers.
     */
    private boolean handleShopConfig(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bedwars.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }
        if (args.length < 7) {
            sender.sendMessage(PREFIX + ChatColor.RED
                    + "Utilisation: /bd shop <catégorie> <slot> <item> <quantité> <prix> <minerai>");
            sender.sendMessage(ChatColor.GRAY + "Exemple: /bd shop blocks 9 WHITE_WOOL 16 4 fer");
            return true;
        }

        String category = args[1].toLowerCase();
        if (category.equals(com.bedwars.shop.ShopConfigManager.RESERVED_TOOLS_CATEGORY)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "\"tools\" est une catégorie réservée (pioche/hache à paliers), non configurable.");
            return true;
        }

        int slot;
        try {
            slot = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Le slot doit être un nombre (9 à 44).");
            return true;
        }
        if (slot < 9 || slot > 44) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Le slot doit être compris entre 9 et 44.");
            return true;
        }

        org.bukkit.Material material;
        try {
            material = org.bukkit.Material.valueOf(args[3].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Item inconnu: " + args[3] + " (nom de Material Bukkit attendu, ex: WHITE_WOOL).");
            return true;
        }

        int amount;
        int price;
        try {
            amount = Integer.parseInt(args[4]);
            price = Integer.parseInt(args[5]);
        } catch (NumberFormatException e) {
            sender.sendMessage(PREFIX + ChatColor.RED + "La quantité et le prix doivent être des nombres entiers.");
            return true;
        }
        if (amount < 1 || price < 1) {
            sender.sendMessage(PREFIX + ChatColor.RED + "La quantité et le prix doivent être supérieurs à 0.");
            return true;
        }

        GeneratorType currencyType = GeneratorType.fromInput(args[6]);
        if (currencyType == null) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Minerai inconnu: " + args[6] + ". Utilisez: fer, or, diamand, emeraude.");
            return true;
        }

        plugin.getShopConfigManager().setItem(category, slot,
                new com.bedwars.shop.ShopItem(material, amount, currencyType.getMaterial(), price));

        sender.sendMessage(PREFIX + ChatColor.GREEN + "Article configuré: " + ChatColor.WHITE + amount + "x " + material.name()
                + ChatColor.GREEN + " au slot " + slot + " de la catégorie " + ChatColor.YELLOW + category
                + ChatColor.GREEN + " (" + price + " " + args[6] + ").");
        return true;
    }

    /**
     * /bd copy <arène source> <nouveau nom> : clone intégralement une arène déjà configurée
     * vers une nouvelle, en translatant tous les points (zone de jeu, lits, spawns, PNJ,
     * générateurs...) par rapport à la position du joueur — un peu comme un "coller" WorldEdit.
     * Le joueur doit se tenir à l'endroit où la structure a été reconstruite à l'identique
     * (au même emplacement relatif que le point /bd <source> spec de l'arène d'origine).
     */
    private boolean handleCopy(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bedwars.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Cette action doit être exécutée en jeu.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd copy <arène source> <nouveau nom>");
            sender.sendMessage(ChatColor.GRAY + "Tenez-vous à l'endroit où la structure a été reconstruite "
                    + "(même point relatif que le /bd <source> spec d'origine) avant d'exécuter la commande.");
            return true;
        }

        String sourceName = args[1];
        String newName = args[2];

        ArenaManager.CopyResult result = plugin.getArenaManager().copyArena(sourceName, newName, player.getLocation());
        switch (result) {
            case SOURCE_NOT_FOUND -> player.sendMessage(PREFIX + ChatColor.RED + "Arène source inconnue: " + sourceName);
            case TARGET_ALREADY_EXISTS -> player.sendMessage(PREFIX + ChatColor.RED + "Une arène nommée " + newName + " existe déjà.");
            case SOURCE_HAS_NO_SPEC -> player.sendMessage(PREFIX + ChatColor.RED
                    + "L'arène source n'a pas de centre de lobby défini (/bd " + sourceName + " spec), impossible de la copier.");
            case INVALID_NAME -> player.sendMessage(PREFIX + ChatColor.RED + "Nom invalide.");
            case SUCCESS -> {
                Arena copy = plugin.getArenaManager().getArena(newName);
                plugin.getShopNpcManager().spawnForArena(copy);
                if (copy.isSaved()) {
                    player.sendMessage(PREFIX + ChatColor.GREEN + "Arène " + ChatColor.YELLOW + newName
                            + ChatColor.GREEN + " créée à partir de " + sourceName + " et directement jouable !");
                } else {
                    player.sendMessage(PREFIX + ChatColor.YELLOW + "Arène " + newName + " créée à partir de " + sourceName
                            + ", mais il manque encore des éléments (vérifiez avec /bd " + newName + " save).");
                }
            }
        }
        return true;
    }

    private void deleteArena(Arena arena) {
        // Renvoie tout le monde (joueurs en attente, en jeu, spectateurs) avant de tout nettoyer.
        var instance = plugin.getGameManager().getExistingInstance(arena);
        if (instance != null) {
            for (Player p : instance.getAllParticipants()) {
                p.sendMessage(PREFIX + ChatColor.RED + "La map " + arena.getName() + " a été supprimée, vous êtes replacé au spawn.");
                p.setGameMode(org.bukkit.GameMode.SURVIVAL);
                if (p.getWorld().getSpawnLocation() != null) {
                    p.teleport(p.getWorld().getSpawnLocation());
                }
            }
        }
        plugin.getGameManager().removeInstance(arena);
        plugin.getWaitingLobbyManager().destroy(arena);
        plugin.getShopNpcManager().removeForArena(arena);
        plugin.getArenaManager().deleteArena(arena);
    }

    /** /bd arene gui : ouvre directement le GUI de sélection des arènes pour celui qui tape la commande. */
    private boolean handleArene(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("clean")) {
            return handleAreneClean(sender);
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Cette action doit être exécutée en jeu.");
            return true;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("gui")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd arene gui" + ChatColor.DARK_GRAY
                    + " (ou /bd arene clean pour purger les PNJ fantômes)");
            return true;
        }
        plugin.getAdminGUIManager().open(player);
        return true;
    }

    /**
     * /bd arene clean : supprime immédiatement tout PNJ marchand/amélioration résiduel
     * (doublons laissés par d'anciens redémarrages) puis les fait proprement réapparaître.
     * Le nettoyage se fait aussi automatiquement à chaque démarrage du plugin ; cette commande
     * permet de le relancer à la demande sans redémarrer le serveur.
     */
    private boolean handleAreneClean(CommandSender sender) {
        if (!sender.hasPermission("bedwars.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }
        plugin.getAdminNPCManager().purgeLegacyNpc();
        int count = 0;
        for (Arena arena : plugin.getArenaManager().getArenas().values()) {
            if (!arena.isSaved()) continue;
            plugin.getShopNpcManager().spawnForArena(arena);
            count++;
        }
        sender.sendMessage(PREFIX + ChatColor.GREEN + "PNJ nettoyés et réaffichés pour " + count + " arène(s).");
        return true;
    }

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Cette action doit être exécutée en jeu.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd join <nom>");
            return true;
        }
        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Arène inconnue: " + args[1]);
            return true;
        }
        if (!arena.isSaved()) {
            player.sendMessage(PREFIX + ChatColor.RED + "Cette arène n'est pas encore prête à être jouée.");
            return true;
        }
        if (!plugin.getGameManager().joinArena(player, arena)) {
            player.sendMessage(PREFIX + ChatColor.RED + "Impossible de rejoindre cette partie (pleine ou déjà lancée).");
        }
        return true;
    }

    /** /bd spectate <nom> : rejoint une partie en cours en tant que spectateur. */
    private boolean handleSpectate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Cette action doit être exécutée en jeu.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd spectate <nom>");
            return true;
        }
        Arena arena = plugin.getArenaManager().getArena(args[1]);
        if (arena == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Arène inconnue: " + args[1]);
            return true;
        }
        if (arena.getState() != ArenaState.PLAYING && arena.getState() != ArenaState.SUDDEN_DEATH) {
            player.sendMessage(PREFIX + ChatColor.RED + "Cette partie n'est pas en cours.");
            return true;
        }
        var instance = plugin.getGameManager().getExistingInstance(arena);
        if (instance == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Cette partie n'est pas en cours.");
            return true;
        }
        instance.toSpectator(player);
        player.sendMessage(PREFIX + ChatColor.GREEN + "Vous observez maintenant la partie " + ChatColor.YELLOW + arena.getName()
                + ChatColor.GREEN + " en spectateur.");
        return true;
    }

    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) return true;
        if (!plugin.getGameManager().leave(player)) {
            player.sendMessage(PREFIX + ChatColor.RED + "Vous n'êtes dans aucune partie.");
            return true;
        }
        player.sendMessage(PREFIX + "Vous avez quitté la partie.");
        return true;
    }

    private static final java.util.Map<String, String> QUICKMENU_ALIASES = java.util.Map.of(
            "epee", com.bedwars.util.PlayerPrefsManager.SWORD,
            "épée", com.bedwars.util.PlayerPrefsManager.SWORD,
            "pioche", com.bedwars.util.PlayerPrefsManager.PICKAXE,
            "hache", com.bedwars.util.PlayerPrefsManager.AXE,
            "bloc", com.bedwars.util.PlayerPrefsManager.BLOCK
    );

    /**
     * /bd quickmenu <épée|pioche|hache|bloc> <0-8> : chaque joueur peut choisir lui-même dans
     * quel slot de sa hotbar il retrouvera son épée, sa pioche, sa hache (en partie) et le bloc
     * "choisir son équipe" (dans le lobby d'attente). Préférence personnelle, sauvegardée.
     */
    private boolean handleQuickMenu(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Cette action doit être exécutée en jeu.");
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd quickmenu <épée|pioche|hache|bloc> <0-8>");
            return true;
        }
        String key = QUICKMENU_ALIASES.get(args[1].toLowerCase());
        if (key == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Item inconnu. Utilisez: épée, pioche, hache, bloc.");
            return true;
        }
        int slot;
        try {
            slot = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(PREFIX + ChatColor.RED + "Le slot doit être un nombre entre 0 et 8.");
            return true;
        }
        if (slot < 0 || slot > 8) {
            player.sendMessage(PREFIX + ChatColor.RED + "Le slot doit être compris entre 0 et 8.");
            return true;
        }

        // On évite qu'un même joueur assigne deux items différents au même slot.
        var prefs = plugin.getPlayerPrefsManager();
        for (String otherKey : new String[]{com.bedwars.util.PlayerPrefsManager.SWORD, com.bedwars.util.PlayerPrefsManager.AXE,
                com.bedwars.util.PlayerPrefsManager.PICKAXE, com.bedwars.util.PlayerPrefsManager.BLOCK}) {
            if (otherKey.equals(key)) continue;
            if (prefs.getSlot(player.getUniqueId(), otherKey, defaultSlotFor(otherKey)) == slot) {
                player.sendMessage(PREFIX + ChatColor.RED + "Ce slot est déjà utilisé par un autre item de votre quickmenu.");
                return true;
            }
        }

        prefs.setSlot(player.getUniqueId(), key, slot);
        player.sendMessage(PREFIX + ChatColor.GREEN + "Position personnalisée enregistrée : " + args[1].toLowerCase()
                + " -> slot " + (slot + 1) + " de la hotbar." + ChatColor.GRAY + " Prend effet à la prochaine partie/au prochain lobby.");
        return true;
    }

    private int defaultSlotFor(String key) {
        return switch (key) {
            case com.bedwars.util.PlayerPrefsManager.SWORD -> com.bedwars.util.KitProtectionUtil.SLOT_SWORD;
            case com.bedwars.util.PlayerPrefsManager.AXE -> com.bedwars.util.KitProtectionUtil.SLOT_AXE;
            case com.bedwars.util.PlayerPrefsManager.PICKAXE -> com.bedwars.util.KitProtectionUtil.SLOT_PICKAXE;
            default -> com.bedwars.util.LobbyItemUtil.SLOT_TEAM_SELECT;
        };
    }

    private boolean handleList(CommandSender sender) {
        List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().getArenas().values());
        if (arenas.isEmpty()) {
            sender.sendMessage(PREFIX + "Aucune arène créée.");
            return true;
        }
        sender.sendMessage(PREFIX + "Arènes (" + arenas.size() + "):");
        for (Arena a : arenas) {
            sender.sendMessage(ChatColor.GRAY + " - " + a.getName() + ChatColor.DARK_GRAY + " ["
                    + a.getState() + ", " + a.getCurrentPlayerCount() + "/" + a.getMaxPlayers() + "]");
        }
        return true;
    }

    // -----------------------------------------------------------------
    // Sous-commandes de configuration d'une arène
    // -----------------------------------------------------------------

    private void handleEquipe(Player player, Arena arena, String[] args) {
        if (args.length < 4) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd " + arena.getName() + " equipe <2/4/6/8> <joueurs par équipe>");
            return;
        }
        int teamCount;
        int playersPerTeam;
        try {
            teamCount = Integer.parseInt(args[2]);
            playersPerTeam = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(PREFIX + ChatColor.RED + "Les nombres d'équipes et de joueurs doivent être des entiers.");
            return;
        }
        if (teamCount < 2 || teamCount > 8 || teamCount % 2 != 0) {
            player.sendMessage(PREFIX + ChatColor.RED + "Le nombre d'équipes doit être pair et compris entre 2 et 8.");
            return;
        }
        if (playersPerTeam < 2 || playersPerTeam > 16) {
            player.sendMessage(PREFIX + ChatColor.RED + "Le nombre de joueurs par équipe doit être compris entre 2 et 16.");
            return;
        }
        arena.setTeamCount(teamCount);
        arena.setPlayersPerTeam(playersPerTeam);
        for (TeamColor color : TeamColor.forTeamCount(teamCount)) {
            arena.getOrCreateTeam(color);
        }
        player.sendMessage(PREFIX + ChatColor.GREEN + teamCount + " équipes de " + playersPerTeam
                + " joueurs configurées pour " + arena.getName() + ".");
        StringBuilder colors = new StringBuilder();
        for (TeamColor color : TeamColor.forTeamCount(teamCount)) {
            colors.append(color.getColoredName()).append(ChatColor.RESET).append(", ");
        }
        player.sendMessage(PREFIX + "Couleurs: " + colors);
    }

    private void handleBed(Player player, Arena arena, String[] args) {
        if (args.length < 3) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd " + arena.getName() + " bed <couleur>");
            return;
        }
        TeamColor color = resolveColor(player, arena, args[2]);
        if (color == null) return;

        // Important : on capture le bloc du LIT visé (raytrace), pas la position du joueur qui
        // tape la commande — sinon la casse de lit ne fonctionne jamais en jeu (le bloc cassé ne
        // correspond à aucune position enregistrée, donc il est traité comme un bloc protégé normal).
        org.bukkit.block.Block target = player.getTargetBlockExact(10);
        if (target == null || !com.bedwars.util.BedUtil.isBedBlock(target.getType())) {
            player.sendMessage(PREFIX + ChatColor.RED + "Regardez directement le lit (bloc, à moins de 10 blocs) avant de taper cette commande.");
            return;
        }

        ArenaTeam team = arena.getOrCreateTeam(color);
        team.setBedLocation(target.getLocation());
        player.sendMessage(PREFIX + ChatColor.GREEN + "Lit de l'équipe " + color.getColoredName()
                + ChatColor.GREEN + " défini.");
    }

    private void handleSpawn(Player player, Arena arena, String[] args) {
        if (args.length < 3) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd " + arena.getName() + " spawn <couleur>");
            return;
        }
        TeamColor color = resolveColor(player, arena, args[2]);
        if (color == null) return;
        ArenaTeam team = arena.getOrCreateTeam(color);
        team.setSpawnLocation(player.getLocation());
        player.sendMessage(PREFIX + ChatColor.GREEN + "Spawn de l'équipe " + color.getColoredName()
                + ChatColor.GREEN + " défini.");
    }

    private void handleItem(Player player, Arena arena, String[] args) {
        if (args.length < 3) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd " + arena.getName() + " item <diamand|emeraude>");
            return;
        }
        GeneratorType type = GeneratorType.fromInput(args[2]);
        if (type == null || type == GeneratorType.FER || type == GeneratorType.OR) {
            player.sendMessage(PREFIX + ChatColor.RED + "Type de minerai inconnu. Utilisez: diamand, emeraude."
                    + ChatColor.GRAY + " (le fer et l'or d'une équipe se configurent désormais via /bd " + arena.getName() + " forge <couleur>)");
            return;
        }
        Generator generator = new Generator(type, player.getLocation());
        arena.getGenerators().add(generator);
        player.sendMessage(PREFIX + ChatColor.GREEN + "Générateur de " + args[2] + " ajouté à " + arena.getName() + ".");
    }

    /**
     * /bd <nom> forge <couleur> : définit le point d'ancrage de la "forge de base" d'une équipe,
     * et crée directement à cet endroit ses générateurs de fer et d'or (remplace les anciens
     * /bd <nom> item fer/or <couleur>, qui ne gère plus que diamant/émeraude). À partir du
     * palier 3 de l'amélioration Forge, du diamant (puis de l'émeraude au palier 4) apparaît
     * aussi directement ici, en plus de l'accélération du fer/or (voir GameInstance).
     */
    private void handleForge(Player player, Arena arena, String[] args) {
        if (args.length < 3) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd " + arena.getName() + " forge <couleur d'équipe>");
            return;
        }
        TeamColor color = resolveColor(player, arena, args[2]);
        if (color == null) return;
        ArenaTeam team = arena.getOrCreateTeam(color);

        // Si une forge existait déjà pour cette équipe, on retire ses anciens générateurs fer/or
        // avant d'en recréer de nouveaux au nouvel emplacement (évite les doublons).
        arena.getGenerators().removeIf(g -> g.getTeam() == color && (g.getType() == GeneratorType.FER || g.getType() == GeneratorType.OR));

        Location loc = player.getLocation();
        team.setForgeLocation(loc);

        Generator iron = new Generator(GeneratorType.FER, loc);
        iron.setTeam(color);
        Generator gold = new Generator(GeneratorType.OR, loc);
        gold.setTeam(color);
        arena.getGenerators().add(iron);
        arena.getGenerators().add(gold);

        player.sendMessage(PREFIX + ChatColor.GREEN + "Forge de base définie pour l'équipe " + color.getColoredName()
                + ChatColor.GREEN + " (générateurs de fer et d'or créés ici, accélérés par l'amélioration Forge).");
    }

    /**
     * /bd <nom> chest <couleur> : ajoute le coffre visé (regardé, à moins de 10 blocs) à la liste
     * des coffres de cette équipe. Une équipe peut avoir plusieurs coffres ; ils sont tous vidés
     * automatiquement au début et à la fin de chaque partie.
     */
    private void handleChest(Player player, Arena arena, String[] args) {
        if (args.length < 3) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd " + arena.getName() + " chest <couleur d'équipe>");
            player.sendMessage(ChatColor.GRAY + "Regardez directement le coffre à moins de 10 blocs avant de taper cette commande.");
            return;
        }
        TeamColor color = resolveColor(player, arena, args[2]);
        if (color == null) return;

        org.bukkit.block.Block target = player.getTargetBlockExact(10);
        if (target == null || !(target.getState() instanceof org.bukkit.block.Chest)) {
            player.sendMessage(PREFIX + ChatColor.RED + "Regardez directement un coffre (à moins de 10 blocs) avant de taper cette commande.");
            return;
        }

        ArenaTeam team = arena.getOrCreateTeam(color);
        Location loc = target.getLocation();
        if (team.getChestLocations().stream().anyMatch(l -> l.getBlockX() == loc.getBlockX()
                && l.getBlockY() == loc.getBlockY() && l.getBlockZ() == loc.getBlockZ())) {
            player.sendMessage(PREFIX + ChatColor.YELLOW + "Ce coffre est déjà enregistré pour cette équipe.");
            return;
        }
        team.getChestLocations().add(loc);
        player.sendMessage(PREFIX + ChatColor.GREEN + "Coffre ajouté à l'équipe " + color.getColoredName()
                + ChatColor.GREEN + " (" + team.getChestLocations().size() + " au total). Il sera vidé au début et à la fin de chaque partie.");
    }

    /** /bd <nom> geninfo : liste tous les générateurs de l'arène (type, équipe, position) pour diagnostiquer. */
    private void handleGenInfo(Player player, Arena arena) {
        if (arena.getGenerators().isEmpty()) {
            player.sendMessage(PREFIX + ChatColor.RED + "Aucun générateur configuré sur " + arena.getName() + ".");
            return;
        }
        player.sendMessage(PREFIX + ChatColor.YELLOW + arena.getGenerators().size() + " générateur(s) sur " + arena.getName() + ":");
        int i = 1;
        for (Generator gen : arena.getGenerators()) {
            Location loc = gen.getLocation();
            String pos = loc == null ? "?" : (int) loc.getX() + "," + (int) loc.getY() + "," + (int) loc.getZ();
            String teamInfo = gen.getTeam() != null ? " - équipe " + gen.getTeam().getColoredName() : ChatColor.GRAY + " (commun)";
            player.sendMessage(ChatColor.GRAY + " " + i + ". " + ChatColor.WHITE + gen.getType() + ChatColor.GRAY + " @ " + pos + teamInfo);
            i++;
        }
    }

    /**
     * /bd <nom> reset : réinitialise immédiatement l'arène quel que soit son état (lobby
     * d'attente, compte à rebours, partie en cours...) — tous les joueurs/spectateurs sont
     * renvoyés au spawn du monde, la map est entièrement restaurée, et l'arène redevient
     * disponible pour une nouvelle partie.
     */
    private void handleReset(Player player, Arena arena) {
        var instance = plugin.getGameManager().getExistingInstance(arena);
        if (instance == null) {
            player.sendMessage(PREFIX + ChatColor.YELLOW + "Aucune partie/lobby en cours sur " + arena.getName() + ", rien à réinitialiser.");
            return;
        }
        instance.forceReset();
        player.sendMessage(PREFIX + ChatColor.GREEN + "Arène " + ChatColor.YELLOW + arena.getName()
                + ChatColor.GREEN + " réinitialisée : tous les joueurs ont été renvoyés, la map a été restaurée.");
    }

    private void handleShop(Player player, Arena arena, String[] args) {
        if (args.length < 5 || !args[3].equalsIgnoreCase("color")) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd " + arena.getName()
                    + " shop <shop|upgrade> color <couleur> [pseudo pour le skin]");
            return;
        }
        boolean isUpgrade = args[2].equalsIgnoreCase("upgrade");
        boolean isShop = args[2].equalsIgnoreCase("shop");
        if (!isUpgrade && !isShop) {
            player.sendMessage(PREFIX + ChatColor.RED + "Le type doit être 'shop' ou 'upgrade'.");
            return;
        }
        TeamColor color = resolveColor(player, arena, args[4]);
        if (color == null) return;
        ArenaTeam team = arena.getOrCreateTeam(color);
        Location loc = player.getLocation();
        String skinName = args.length >= 6 ? args[5] : null;

        var type = isShop ? com.bedwars.npc.ShopNpcManager.NpcType.SHOP : com.bedwars.npc.ShopNpcManager.NpcType.UPGRADE;
        if (isShop) {
            team.setShopLocation(loc);
        } else {
            team.setUpgradeLocation(loc);
        }
        plugin.getShopNpcManager().spawnOne(arena, color, type, loc, skinName);

        player.sendMessage(PREFIX + ChatColor.GREEN + (isShop ? "Marchand" : "PNJ d'amélioration")
                + " placé pour l'équipe " + color.getColoredName() + ChatColor.GREEN + ".");
    }

    /** /bd <nom> minplayers <nombre> : nombre de joueurs minimum pour lancer le compte à rebours. */
    private void handleMinPlayers(Player player, Arena arena, String[] args) {
        if (args.length < 3) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd " + arena.getName() + " minplayers <nombre|off>");
            return;
        }
        if (args[2].equalsIgnoreCase("off")) {
            arena.setMinPlayers(-1);
            player.sendMessage(PREFIX + ChatColor.GREEN + "Minimum de joueurs désactivé : il faudra que le lobby soit plein "
                    + "(" + arena.getMaxPlayers() + " joueurs) pour lancer automatiquement la partie.");
            return;
        }
        int value;
        try {
            value = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(PREFIX + ChatColor.RED + "Le nombre de joueurs doit être un nombre entier (ou 'off').");
            return;
        }
        if (value < 1) {
            player.sendMessage(PREFIX + ChatColor.RED + "Le nombre de joueurs minimum doit être supérieur à 0.");
            return;
        }
        arena.setMinPlayers(value);
        player.sendMessage(PREFIX + ChatColor.GREEN + "Nombre de joueurs minimum défini à " + ChatColor.YELLOW + value
                + ChatColor.GREEN + " pour " + arena.getName() + " (le compte à rebours démarrera dès ce seuil atteint).");
    }

    private void handleSave(Player player, Arena arena) {
        List<String> missing = arena.getMissingRequirements();
        if (!missing.isEmpty()) {
            player.sendMessage(PREFIX + ChatColor.RED + "Impossible de sauvegarder " + arena.getName()
                    + ", il manque:");
            for (String m : missing) {
                player.sendMessage(ChatColor.RED + " - " + m);
            }
            return;
        }
        plugin.getArenaManager().captureRegion(arena);
        arena.setSaved(true);
        arena.setState(ArenaState.WAITING);
        plugin.getShopNpcManager().spawnForArena(arena);
        player.sendMessage(PREFIX + ChatColor.GREEN + "Arène " + arena.getName() + " sauvegardée et jouable !");
    }

    private TeamColor resolveColor(Player player, Arena arena, String input) {
        TeamColor color = TeamColor.fromDisplayName(input);
        if (color == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Couleur inconnue: " + input);
            return null;
        }
        if (arena.getTeamCount() > 0) {
            List<TeamColor> valid = List.of(TeamColor.forTeamCount(arena.getTeamCount()));
            if (!valid.contains(color)) {
                player.sendMessage(PREFIX + ChatColor.RED + "Cette arène n'a pas d'équipe " + color.getColoredName()
                        + ChatColor.RED + ". Configurez d'abord /bd " + arena.getName() + " equipe ...");
                return null;
            }
        }
        return color;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(PREFIX + ChatColor.YELLOW + "Commandes disponibles:");
        sender.sendMessage(ChatColor.GRAY + "/bd create <nom>  |  /bd delete <nom>");
        sender.sendMessage(ChatColor.GRAY + "/bd copy <source> <nouveau nom>" + ChatColor.DARK_GRAY + " (clone une arène, translatée à votre position)");
        sender.sendMessage(ChatColor.GRAY + "/bd shop <catégorie> <slot> <item> <quantité> <prix> <minerai>");
        sender.sendMessage(ChatColor.GRAY + "/bd arene gui" + ChatColor.DARK_GRAY + " (affiche directement le GUI des arènes)");
        sender.sendMessage(ChatColor.GRAY + "/bd arene clean" + ChatColor.DARK_GRAY + " (purge les PNJ marchand/amélioration fantômes)");
        sender.sendMessage(ChatColor.GRAY + "/bd join <nom>  |  /bd spectate <nom>  |  /bd leave  |  /bd list");
        sender.sendMessage(ChatColor.GRAY + "/bd quickmenu <épée|pioche|hache|bloc> <0-8>" + ChatColor.DARK_GRAY + " (personnalisez vos slots)");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> equipe <2/4/6/8> <joueurs>");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> pos1 | pos2 | posconfirm");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> bed <couleur>");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> spawn <couleur>");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> item <diamand|emeraude>" + ChatColor.DARK_GRAY + " (générateurs communs de la map)");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> forge <couleur>" + ChatColor.DARK_GRAY + " (crée fer+or de l'équipe, accélérés par l'amélioration Forge)");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> chest <couleur>" + ChatColor.DARK_GRAY + " (ajoute le coffre visé à l'équipe, vidé à chaque partie)");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> geninfo" + ChatColor.DARK_GRAY + " (liste tous les générateurs pour diagnostiquer)");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> reset" + ChatColor.DARK_GRAY + " (réinitialise immédiatement une arène, même en pleine partie)");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> shop <shop|upgrade> color <couleur> [pseudo]");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> spec " + ChatColor.DARK_GRAY + "(= centre du lobby d'attente flottant)");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> specspawn" + ChatColor.DARK_GRAY + " (spawn spectateurs pendant la partie, ex: milieu de la map)");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> minplayers <nombre|off>" + ChatColor.DARK_GRAY + " (seuil pour lancer le compte à rebours)");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> save  |  /bd <nom> edit" + ChatColor.DARK_GRAY + " (= config, réactive la modification libre de la map)");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(List.of("create", "delete", "copy", "shop", "arene", "join", "spectate", "leave", "quickmenu", "list"));
            options.addAll(plugin.getArenaManager().getArenas().keySet());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("arene")) {
                options.addAll(List.of("gui", "clean"));
            } else if (args[0].equalsIgnoreCase("quickmenu")) {
                options.addAll(List.of("epee", "pioche", "hache", "bloc"));
            } else if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("delete")
                    || args[0].equalsIgnoreCase("copy") || args[0].equalsIgnoreCase("spectate")) {
                options.addAll(plugin.getArenaManager().getArenas().keySet());
            } else if (args[0].equalsIgnoreCase("shop")) {
                options.addAll(plugin.getShopConfigManager().getCategories());
                options.addAll(List.of("blocks", "melee", "armor", "ranged", "potions", "utility"));
            } else if (plugin.getArenaManager().getArena(args[0]) != null) {
                options.addAll(List.of("equipe", "pos1", "pos2", "posconfirm", "bed", "spawn",
                        "item", "forge", "chest", "geninfo", "reset", "shop", "spec", "specspawn", "minplayers", "save", "edit", "config"));
            }
        } else if (args.length == 7 && args[0].equalsIgnoreCase("shop")) {
            options.addAll(List.of("fer", "or", "diamand", "emeraude"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("quickmenu")) {
            options.addAll(List.of("0", "1", "2", "3", "4", "5", "6", "7", "8"));
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("delete")) {
                options.add("confirm");
            }
            Arena arena = plugin.getArenaManager().getArena(args[0]);
            if (arena != null) {
                switch (args[1].toLowerCase()) {
                    case "bed", "spawn", "forge", "chest" -> options.addAll(colorNames());
                    case "item" -> options.addAll(List.of("diamand", "emeraude"));
                    case "shop" -> options.addAll(List.of("shop", "upgrade"));
                }
            }
        } else if (args.length == 4 && args[1].equalsIgnoreCase("shop")) {
            options.add("color");
        } else if (args.length == 5 && args[1].equalsIgnoreCase("shop")) {
            options.addAll(colorNames());
        }

        String current = args[args.length - 1].toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(current)).collect(Collectors.toList());
    }

    private List<String> colorNames() {
        List<String> names = new ArrayList<>();
        for (TeamColor c : TeamColor.values()) names.add(c.getDisplayName());
        return names;
    }
}
