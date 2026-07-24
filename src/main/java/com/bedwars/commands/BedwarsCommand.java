package com.bedwars.commands;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.*;
import com.bedwars.game.GameInstance;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BedwarsCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = ChatColor.GOLD + "[Bedwars] " + ChatColor.RESET;

    private final BedwarsPlugin plugin;

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
        if (sub.equals("admin")) {
            return handleAdmin(sender, args);
        }
        if (sub.equals("join")) {
            return handleJoin(sender, args);
        }
        if (sub.equals("leave")) {
            return handleLeave(sender);
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
            case "shop" -> handleShop(player, arena, args);
            case "spec" -> {
                arena.setSpecLocation(player.getLocation());
                player.sendMessage(PREFIX + "Spawn des spectateurs défini pour " + arena.getName() + ".");
            }
            case "lobby" -> handleLobby(player, arena, args);
            case "save" -> handleSave(player, arena);
            case "config" -> {
                arena.setSaved(false);
                arena.setState(ArenaState.SETUP);
                player.sendMessage(PREFIX + ChatColor.YELLOW + "Mode configuration activé pour " + arena.getName()
                        + ". Refaites /bd " + arena.getName() + " save une fois vos modifications terminées.");
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

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bedwars.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Vous n'avez pas la permission.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Cette action doit être exécutée en jeu.");
            return true;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("gui")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd admin gui");
            return true;
        }
        plugin.getAdminNPCManager().spawnAt(player.getLocation());
        player.sendMessage(PREFIX + ChatColor.GREEN + "NPC d'administration placé ici. Clique dessus pour ouvrir le menu.");
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

    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) return true;
        GameInstance instance = plugin.getGameManager().findInstanceOf(player);
        if (instance == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Vous n'êtes dans aucune partie.");
            return true;
        }
        instance.removeWaitingPlayer(player);
        player.sendMessage(PREFIX + "Vous avez quitté la partie.");
        return true;
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
        ArenaTeam team = arena.getOrCreateTeam(color);
        team.setBedLocation(player.getLocation());
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
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd " + arena.getName() + " item <fer|or|diamand|emeraude>");
            return;
        }
        GeneratorType type = GeneratorType.fromInput(args[2]);
        if (type == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "Type de minerai inconnu. Utilisez: fer, or, diamand, emeraude.");
            return;
        }
        Generator generator = new Generator(type, player.getLocation());
        arena.getGenerators().add(generator);
        player.sendMessage(PREFIX + ChatColor.GREEN + "Générateur de " + args[2] + " ajouté à " + arena.getName() + ".");
    }

    private void handleShop(Player player, Arena arena, String[] args) {
        if (args.length < 5 || !args[3].equalsIgnoreCase("color")) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd " + arena.getName() + " shop <shop|upgrade> color <couleur>");
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

        if (isShop) {
            team.setShopLocation(loc);
        } else {
            team.setUpgradeLocation(loc);
        }

        Villager villager = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setCollidable(false);
        villager.setPersistent(true);
        villager.setProfession(isShop ? Villager.Profession.WEAPONSMITH : Villager.Profession.LIBRARIAN);
        villager.setCustomName(color.getChatColor() + (isShop ? "Marchand" : "Amélioration") + " " + color.getDisplayName());
        villager.setCustomNameVisible(true);

        player.sendMessage(PREFIX + ChatColor.GREEN + (isShop ? "Marchand" : "Villageois d'amélioration")
                + " placé pour l'équipe " + color.getColoredName() + ChatColor.GREEN + ".");
    }

    private void handleLobby(Player player, Arena arena, String[] args) {
        if (args.length < 3) {
            player.sendMessage(PREFIX + ChatColor.RED + "Utilisation: /bd " + arena.getName() + " lobby <pos1|pos2|posconfirm>");
            return;
        }
        switch (args[2].toLowerCase()) {
            case "pos1" -> {
                arena.setLobbyPos1(player.getLocation());
                arena.setLobbyZoneConfirmed(false);
                player.sendMessage(PREFIX + "Position 1 de la zone d'attente définie.");
            }
            case "pos2" -> {
                arena.setLobbyPos2(player.getLocation());
                arena.setLobbyZoneConfirmed(false);
                player.sendMessage(PREFIX + "Position 2 de la zone d'attente définie.");
            }
            case "posconfirm" -> {
                if (arena.getLobbyPos1() == null || arena.getLobbyPos2() == null) {
                    player.sendMessage(PREFIX + ChatColor.RED + "Il faut définir lobby pos1 et pos2 avant de confirmer.");
                } else {
                    arena.setLobbyZoneConfirmed(true);
                    player.sendMessage(PREFIX + ChatColor.GREEN + "Zone d'attente confirmée.");
                }
            }
            default -> player.sendMessage(PREFIX + ChatColor.RED + "Sous-action inconnue pour lobby.");
        }
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
        sender.sendMessage(ChatColor.GRAY + "/bd create <nom>");
        sender.sendMessage(ChatColor.GRAY + "/bd admin gui");
        sender.sendMessage(ChatColor.GRAY + "/bd join <nom>  |  /bd leave  |  /bd list");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> equipe <2/4/6/8> <joueurs>");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> pos1 | pos2 | posconfirm");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> bed <couleur>");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> spawn <couleur>");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> item <fer|or|diamand|emeraude>");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> shop <shop|upgrade> color <couleur>");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> spec");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> lobby pos1 | pos2 | posconfirm");
        sender.sendMessage(ChatColor.GRAY + "/bd <nom> save  |  /bd <nom> config");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(List.of("create", "admin", "join", "leave", "list"));
            options.addAll(plugin.getArenaManager().getArenas().keySet());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("admin")) {
                options.add("gui");
            } else if (args[0].equalsIgnoreCase("join")) {
                options.addAll(plugin.getArenaManager().getArenas().keySet());
            } else if (plugin.getArenaManager().getArena(args[0]) != null) {
                options.addAll(List.of("equipe", "pos1", "pos2", "posconfirm", "bed", "spawn",
                        "item", "shop", "spec", "lobby", "save", "config"));
            }
        } else if (args.length == 3) {
            Arena arena = plugin.getArenaManager().getArena(args[0]);
            if (arena != null) {
                switch (args[1].toLowerCase()) {
                    case "bed", "spawn" -> options.addAll(colorNames());
                    case "item" -> options.addAll(List.of("fer", "or", "diamand", "emeraude"));
                    case "shop" -> options.addAll(List.of("shop", "upgrade"));
                    case "lobby" -> options.addAll(List.of("pos1", "pos2", "posconfirm"));
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
