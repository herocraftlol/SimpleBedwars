package com.bedwars.game;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.*;
import com.bedwars.upgrade.TeamUpgrades;
import com.bedwars.upgrade.TrapType;
import com.bedwars.shop.ToolTier;
import com.bedwars.shop.SwordTier;
import com.bedwars.util.KitProtectionUtil;
import com.bedwars.util.LobbyItemUtil;
import org.bukkit.*;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GameInstance {

    private final BedwarsPlugin plugin;
    private final Arena arena;

    private final Map<UUID, TeamColor> playerTeams = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> finalKills = new HashMap<>();
    private final Map<Generator, List<UUID>> groundItems = new HashMap<>();
    private final Set<Location> placedBlocks = new HashSet<>();
    private final Map<TeamColor, TeamUpgrades> teamUpgrades = new EnumMap<>(TeamColor.class);
    private final Map<UUID, Integer> pickaxeTier = new HashMap<>();
    private final Map<UUID, Integer> axeTier = new HashMap<>();
    private final Map<UUID, Integer> swordTier = new HashMap<>();
    /** Équipe demandée par un joueur dans le lobby (voir l'item "Choisir son équipe"), avant le lancement. */
    private final Map<UUID, TeamColor> preferredTeam = new HashMap<>();

    private BukkitTask lobbyCountdownTask;
    private int lobbyCountdown;

    private BukkitTask mainTimerTask;
    private BukkitTask ironGoldTask;
    private final Map<Generator, BukkitTask> preciousTasks = new HashMap<>();
    private BukkitTask forgeBonusTask;
    private final Map<TeamColor, Long> forgeDiamondCounter = new EnumMap<>(TeamColor.class);
    private final Map<TeamColor, Long> forgeEmeraldCounter = new EnumMap<>(TeamColor.class);
    private BukkitTask scoreboardTask;
    private BukkitTask upgradeEffectsTask;

    private int elapsedSeconds = 0;
    private int phase = 1; // 1 = normal, 2 = à 25min restantes, 3 = à 5min restantes
    private boolean suddenDeathTriggered = false;

    public GameInstance(BedwarsPlugin plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    public Arena getArena() {
        return arena;
    }

    /** Améliorations achetées par l'équipe pour cette partie (créées à la demande). */
    public TeamUpgrades getTeamUpgrades(TeamColor color) {
        return teamUpgrades.computeIfAbsent(color, c -> new TeamUpgrades());
    }

    // ---------------------------------------------------------------
    // Gestion du lobby (avant partie)
    // ---------------------------------------------------------------

    public boolean addPlayer(Player player) {
        if (arena.getState() != ArenaState.WAITING && arena.getState() != ArenaState.STARTING) {
            return false;
        }
        if (arena.isLobbyFull()) return false;

        boolean wasEmpty = arena.getWaitingPlayers().isEmpty();
        arena.getWaitingPlayers().add(player.getUniqueId());

        WaitingLobbyManager lobby = plugin.getWaitingLobbyManager();
        if (wasEmpty) {
            // Premier joueur : on fait apparaître temporairement le lobby flottant au-dessus de la map.
            lobby.build(arena);
        }
        Location center = lobby.getCenter(arena);
        if (center != null) {
            player.teleport(center);
        }
        giveLobbyItems(player);

        broadcastToArena(ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " a rejoint la partie ("
                + arena.getCurrentPlayerCount() + "/" + arena.getMaxPlayers() + ")");
        if (arena.isReadyToStart() && arena.getState() == ArenaState.WAITING) {
            startCountdown();
        }
        return true;
    }

    /** Donne les items spéciaux du lobby d'attente : forcer le lancement (admin), choisir son équipe, quitter. */
    private void giveLobbyItems(Player player) {
        if (player.hasPermission("bedwars.admin")) {
            player.getInventory().setItem(LobbyItemUtil.SLOT_FORCE_START, LobbyItemUtil.createForceStartItem());
        }
        player.getInventory().setItem(LobbyItemUtil.SLOT_TEAM_SELECT, LobbyItemUtil.createTeamSelectItem());
        player.getInventory().setItem(LobbyItemUtil.SLOT_LEAVE, LobbyItemUtil.createLeaveItem());
    }

    public void setPreferredTeam(UUID uuid, TeamColor color) {
        preferredTeam.put(uuid, color);
    }

    public TeamColor getPreferredTeam(UUID uuid) {
        return preferredTeam.get(uuid);
    }

    /** Nombre de joueurs ayant déjà choisi cette équipe (pour l'affichage dans le menu de sélection). */
    public int countPreferred(TeamColor color) {
        int count = 0;
        for (TeamColor c : preferredTeam.values()) {
            if (c == color) count++;
        }
        return count;
    }

    /** Lance la partie immédiatement, peu importe le nombre de joueurs (réservé aux admins, voir le diamant du lobby). */
    public void forceStart(Player initiator) {
        if (arena.getState() != ArenaState.WAITING && arena.getState() != ArenaState.STARTING) return;
        if (arena.getWaitingPlayers().isEmpty()) {
            initiator.sendMessage(ChatColor.RED + "Impossible de lancer : personne n'attend dans le lobby.");
            return;
        }
        if (lobbyCountdownTask != null) lobbyCountdownTask.cancel();
        broadcastToArena(ChatColor.AQUA + "" + ChatColor.BOLD + initiator.getName() + ChatColor.GRAY
                + " force le lancement de la partie !");
        startGame();
    }

    public void removeWaitingPlayer(Player player) {
        arena.getWaitingPlayers().remove(player.getUniqueId());
        preferredTeam.remove(player.getUniqueId());
        if (arena.getState() == ArenaState.STARTING && !arena.isReadyToStart()) {
            cancelCountdown();
        }
        if (arena.getWaitingPlayers().isEmpty() && arena.getState() != ArenaState.PLAYING
                && arena.getState() != ArenaState.SUDDEN_DEATH) {
            // Plus personne n'attend : on retire le lobby flottant.
            plugin.getWaitingLobbyManager().destroy(arena);
        }
    }

    private void startCountdown() {
        arena.setState(ArenaState.STARTING);
        lobbyCountdown = plugin.getConfig().getInt("game.countdown-lobby-seconds", 10);
        lobbyCountdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (lobbyCountdown <= 0) {
                lobbyCountdownTask.cancel();
                startGame();
                return;
            }
            if (lobbyCountdown <= 5 || lobbyCountdown % 5 == 0) {
                broadcastToArena(ChatColor.GOLD + "Départ dans " + ChatColor.YELLOW + lobbyCountdown + ChatColor.GOLD + " secondes...");
            }
            lobbyCountdown--;
        }, 0L, 20L);
    }

    private void cancelCountdown() {
        if (lobbyCountdownTask != null) lobbyCountdownTask.cancel();
        arena.setState(ArenaState.WAITING);
        broadcastToArena(ChatColor.RED + "Un joueur est parti, le lancement est annulé.");
    }

    // ---------------------------------------------------------------
    // Démarrage de partie
    // ---------------------------------------------------------------

    private void startGame() {
        arena.setState(ArenaState.PLAYING);
        plugin.getWaitingLobbyManager().destroy(arena);
        // Sécurité supplémentaire : les améliorations d'équipe repartent toujours de zéro à
        // chaque lancement (elles le sont déjà de fait, chaque partie utilisant une instance
        // fraîche, mais on le garantit explicitement ici).
        teamUpgrades.clear();
        // S'assure que les PNJ marchand/amélioration sont bien présents avant que les joueurs
        // n'arrivent sur la map (ils peuvent avoir disparu entre la configuration et maintenant :
        // chunk déchargé entre-temps, redémarrage du serveur, etc.).
        plugin.getShopNpcManager().spawnForArena(arena);
        assignTeams();

        for (UUID uuid : arena.getWaitingPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            TeamColor color = playerTeams.get(uuid);
            ArenaTeam team = arena.getTeams().get(color);
            if (team != null && team.getSpawnLocation() != null) {
                player.teleport(team.getSpawnLocation());
            }
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            giveKit(player, color);
        }

        startGenerators();
        startMainTimer();
        startUpgradeEffects();
        startForgeBonusResources();
        scoreboardTask = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> plugin.getScoreboardManager().update(this), 0L, 20L);

        broadcastToArena(ChatColor.GREEN + "" + ChatColor.BOLD + "La partie commence !");
    }

    private void assignTeams() {
        TeamColor[] colors = TeamColor.forTeamCount(arena.getTeamCount());
        int perTeam = Math.max(1, arena.getPlayersPerTeam());
        List<UUID> players = new ArrayList<>(arena.getWaitingPlayers());
        Collections.shuffle(players);

        Map<TeamColor, Integer> counts = new EnumMap<>(TeamColor.class);
        for (TeamColor c : colors) counts.put(c, 0);

        List<UUID> remaining = new ArrayList<>();
        // On honore d'abord les équipes choisies dans le lobby (si elles ne sont pas déjà pleines).
        for (UUID uuid : players) {
            TeamColor preferred = preferredTeam.get(uuid);
            boolean valid = preferred != null && counts.containsKey(preferred) && counts.get(preferred) < perTeam;
            if (valid) {
                assignPlayerToTeam(uuid, preferred);
                counts.put(preferred, counts.get(preferred) + 1);
            } else {
                remaining.add(uuid);
            }
        }
        // Puis on répartit le reste équitablement entre les équipes qui ont encore de la place.
        int index = 0;
        for (UUID uuid : remaining) {
            TeamColor chosen = null;
            for (int i = 0; i < colors.length; i++) {
                TeamColor candidate = colors[(index + i) % colors.length];
                if (counts.get(candidate) < perTeam) {
                    chosen = candidate;
                    break;
                }
            }
            if (chosen == null) chosen = colors[index % colors.length]; // trop de joueurs pour les équipes : cas limite
            assignPlayerToTeam(uuid, chosen);
            counts.put(chosen, counts.getOrDefault(chosen, 0) + 1);
            index++;
        }
        preferredTeam.clear();
    }

    private void assignPlayerToTeam(UUID uuid, TeamColor color) {
        ArenaTeam team = arena.getOrCreateTeam(color);
        team.getMembers().add(uuid);
        team.getAlivePlayers().add(uuid);
        playerTeams.put(uuid, color);
    }

    private void giveKit(Player player, TeamColor color) {
        player.getInventory().setHelmet(dyed(Material.LEATHER_HELMET, color));
        player.getInventory().setChestplate(dyed(Material.LEATHER_CHESTPLATE, color));
        player.getInventory().setLeggings(dyed(Material.LEATHER_LEGGINGS, color));
        player.getInventory().setBoots(dyed(Material.LEATHER_BOOTS, color));

        // Épée / hache / pioche : toujours aux 3 premiers slots de la hotbar (slots 1/2/3),
        // au palier actuel du joueur (bois par défaut, voir downgradeTools). Verrouillés par
        // KitProtectionListener : indroppables, indéplaçables, indupliquables.
        UUID uuid = player.getUniqueId();
        SwordTier sTier = SwordTier.byLevel(getSwordTier(uuid));
        ToolTier aTier = ToolTier.byLevel(getAxeTier(uuid));
        ToolTier pTier = ToolTier.byLevel(getPickaxeTier(uuid));

        player.getInventory().setItem(KitProtectionUtil.SLOT_SWORD,
                KitProtectionUtil.tagAsKitTool(new ItemStack(sTier.getMaterial()), "Épée"));
        player.getInventory().setItem(KitProtectionUtil.SLOT_AXE,
                KitProtectionUtil.tagAsKitTool(new ItemStack(aTier.getAxe()), "Hache"));
        player.getInventory().setItem(KitProtectionUtil.SLOT_PICKAXE,
                KitProtectionUtil.tagAsKitTool(new ItemStack(pTier.getPickaxe()), "Pioche"));

        applySharpnessToSword(player);
    }

    public int getSwordTier(UUID uuid) {
        return swordTier.getOrDefault(uuid, SwordTier.WOOD.getLevel());
    }

    public void setSwordTier(UUID uuid, int level) {
        swordTier.put(uuid, level);
    }

    public int getPickaxeTier(UUID uuid) {
        return pickaxeTier.getOrDefault(uuid, ToolTier.WOOD.getLevel());
    }

    public int getAxeTier(UUID uuid) {
        return axeTier.getOrDefault(uuid, ToolTier.WOOD.getLevel());
    }

    public void setPickaxeTier(UUID uuid, int level) {
        pickaxeTier.put(uuid, level);
    }

    public void setAxeTier(UUID uuid, int level) {
        axeTier.put(uuid, level);
    }

    /** À chaque mort (non finale), épée/pioche/hache redescendent d'un palier (jamais en dessous du bois). */
    private void downgradeTools(UUID uuid) {
        swordTier.put(uuid, Math.max(SwordTier.WOOD.getLevel(), getSwordTier(uuid) - 1));
        pickaxeTier.put(uuid, Math.max(ToolTier.WOOD.getLevel(), getPickaxeTier(uuid) - 1));
        axeTier.put(uuid, Math.max(ToolTier.WOOD.getLevel(), getAxeTier(uuid) - 1));
    }

    /**
     * Remplace le matériau de l'épée du slot protégé par le palier donné, sans perdre le
     * verrouillage (tag) ni l'enchantement Sharpness en cours (voir applySharpnessToSword).
     */
    public void refreshSwordItem(Player player) {
        SwordTier tier = SwordTier.byLevel(getSwordTier(player.getUniqueId()));
        player.getInventory().setItem(KitProtectionUtil.SLOT_SWORD,
                KitProtectionUtil.tagAsKitTool(new ItemStack(tier.getMaterial()), "Épée"));
        applySharpnessToSword(player);
    }

    /**
     * L'amélioration "Sharpened Blades" ne s'applique QUE sur l'épée du slot 1 de la hotbar
     * (pas un effet de potion Force qui boosterait aussi les poings/autres armes) : on pose
     * directement l'enchantement Tranchant correspondant sur cette épée précise.
     */
    public void applySharpnessToSword(Player player) {
        TeamColor color = playerTeams.get(player.getUniqueId());
        int level = 0;
        if (color != null && teamUpgrades.containsKey(color)) {
            level = teamUpgrades.get(color).getSharpenedBlades();
        }
        ItemStack sword = player.getInventory().getItem(KitProtectionUtil.SLOT_SWORD);
        if (sword == null || !KitProtectionUtil.isKitTool(sword)) return;
        if (level > 0) {
            sword.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS, level);
        } else {
            sword.removeEnchantment(org.bukkit.enchantments.Enchantment.SHARPNESS);
        }
        player.getInventory().setItem(KitProtectionUtil.SLOT_SWORD, sword);
    }

    private ItemStack dyed(Material material, TeamColor color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color.getArmorColor());
        item.setItemMeta(meta);
        return item;
    }

    // ---------------------------------------------------------------
    // Effets des améliorations d'équipe (Sharpened Blades, Reinforced Armor,
    // Maniac Miner, Heal Pool, Dragon Buff, pièges)
    // ---------------------------------------------------------------

    private void startUpgradeEffects() {
        upgradeEffectsTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (TeamColor color : TeamColor.forTeamCount(arena.getTeamCount())) {
                ArenaTeam team = arena.getTeams().get(color);
                if (team == null || !teamUpgrades.containsKey(color)) continue;
                TeamUpgrades upgrades = teamUpgrades.get(color);

                for (UUID uuid : team.getAlivePlayers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) continue;

                    // Sharpened Blades : contrairement aux autres effets, ne s'applique QU'à
                    // l'épée du slot 1 de la hotbar (enchantement direct), pas un effet de
                    // potion qui boosterait aussi les poings ou toute autre arme.
                    applySharpnessToSword(p);

                    if (upgrades.getReinforcedArmor() > 0) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,
                                140, Math.max(0, upgrades.getReinforcedArmor() - 1), true, false));
                    }
                    if (upgrades.getManiacMiner() > 0 && team.getBedLocation() != null
                            && p.getLocation().getWorld().equals(team.getBedLocation().getWorld())
                            && p.getLocation().distanceSquared(team.getBedLocation()) <= 40 * 40) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE,
                                140, upgrades.getManiacMiner() - 1, true, false));
                    }
                    if (upgrades.isHealPool() && team.getBedLocation() != null
                            && p.getLocation().getWorld().equals(team.getBedLocation().getWorld())
                            && p.getLocation().distanceSquared(team.getBedLocation()) <= 8 * 8) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 140, 1, true, false));
                    }
                }

                checkTraps(color, team, upgrades);
            }
        }, 20L, 20L);
    }

    /** Déclenche le prochain piège en attente si un ennemi entre dans la zone de base de l'équipe. */
    private void checkTraps(TeamColor ownerColor, ArenaTeam team, TeamUpgrades upgrades) {
        if (upgrades.getTraps().isEmpty() || team.getBedLocation() == null) return;

        Location bed = team.getBedLocation();
        List<Player> intruders = new ArrayList<>();
        for (Map.Entry<UUID, TeamColor> entry : playerTeams.entrySet()) {
            if (entry.getValue() == ownerColor) continue;
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !isAlivePlaying(p)) continue;
            if (p.getLocation().getWorld().equals(bed.getWorld()) && p.getLocation().distanceSquared(bed) <= 12 * 12) {
                intruders.add(p);
            }
        }
        if (intruders.isEmpty()) return;

        TrapType trap = upgrades.pollTrap();
        if (trap == null) return;

        broadcastToArena(ownerColor.getColoredName() + ChatColor.GRAY + " a déclenché " + trap.getColoredName());
        for (Player p : team.getAlivePlayers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList()) {
            p.sendMessage(ChatColor.GOLD + "Piège déclenché : " + trap.getColoredName());
        }

        switch (trap) {
            case ALARM -> {
                for (Player intruder : intruders) {
                    for (UUID uuid : team.getAlivePlayers()) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) p.sendMessage(ChatColor.RED + intruder.getName() + ChatColor.GRAY + " est dans votre base !");
                    }
                    intruder.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0, true, false));
                }
            }
            case COUNTER_OFFENSIVE -> {
                for (UUID uuid : team.getAlivePlayers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p == null) continue;
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 160, 1, true, false));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 160, 1, true, false));
                }
            }
            case ITS_A_TRAP -> {
                for (Player intruder : intruders) {
                    intruder.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 160, 0, true, false));
                    intruder.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 1, true, false));
                }
            }
            case MINER_FATIGUE -> {
                for (Player intruder : intruders) {
                    intruder.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 200, 2, true, false));
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Générateurs de ressources
    // ---------------------------------------------------------------

    private void startGenerators() {
        ironGoldTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Les minerais ne doivent jamais spawn en dehors d'une partie active, ni si plus
            // personne ne joue (sécurité en plus de l'annulation normale des tâches en fin de partie).
            if (!isGameActive()) return;

            for (Generator gen : arena.getGenerators()) {
                if (gen.getType() != GeneratorType.FER && gen.getType() != GeneratorType.OR) continue;
                gen.incrementTick();
                long baseInterval = gen.getType() == GeneratorType.FER
                        ? plugin.getConfig().getLong("generators.iron-interval-ticks", 20)
                        : plugin.getConfig().getLong("generators.gold-interval-ticks", 80);
                long interval = applyForge(gen, baseInterval);
                if (gen.getTickCounter() >= interval) {
                    gen.resetTick();
                    spawnSplitResource(gen);
                }
            }
        }, 20L, 1L);

        for (Generator gen : arena.getGenerators()) {
            if (gen.getType() == GeneratorType.DIAMOND || gen.getType() == GeneratorType.EMERAUDE) {
                schedulePreciousGenerator(gen);
            }
        }
    }

    /** true si une partie est réellement en cours et qu'il y a au moins un joueur en jeu. */
    private boolean isGameActive() {
        if (arena.getState() != ArenaState.PLAYING && arena.getState() != ArenaState.SUDDEN_DEATH) return false;
        for (UUID uuid : playerTeams.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && isAlivePlaying(p)) return true;
        }
        return false;
    }

    /**
     * Le "Forge" (Amélioration d'équipe) accélère la production des générateurs fer/or de la
     * base de l'équipe (voir aussi {@link #startForgeBonusResources()} pour le bonus diamant/émeraude) :
     *  - Palier 1 : x1.25   - Palier 2 : x1.75   - Palier 3 : x2.0   - Palier 4 : x2.25
     */
    private long applyForge(Generator gen, long baseInterval) {
        if (gen.getTeam() == null) return baseInterval;
        int forgeLevel = teamUpgrades.containsKey(gen.getTeam()) ? teamUpgrades.get(gen.getTeam()).getForge() : 0;
        double multiplier = forgeSpeedMultiplier(forgeLevel);
        if (multiplier <= 1.0) return baseInterval;
        return Math.max(1, Math.round(baseInterval / multiplier));
    }

    private double forgeSpeedMultiplier(int forgeLevel) {
        return switch (forgeLevel) {
            case 1 -> 1.25;
            case 2 -> 1.75;
            case 3 -> 2.0;
            case 4 -> 2.25;
            default -> 1.0;
        };
    }

    /**
     * À partir du palier 3 de la Forge, du diamant apparaît directement au point
     * {@code ArenaTeam#getForgeLocation()} de l'équipe (1 par minute), et au palier 4 le diamant
     * accélère à 1 toutes les 15 secondes tout en ajoutant de l'émeraude (1 toutes les 2 minutes).
     */
    private void startForgeBonusResources() {
        forgeBonusTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isGameActive()) return;

            for (Map.Entry<TeamColor, ArenaTeam> entry : arena.getTeams().entrySet()) {
                TeamColor color = entry.getKey();
                ArenaTeam team = entry.getValue();
                if (team.getForgeLocation() == null) continue;
                TeamUpgrades upgrades = teamUpgrades.get(color);
                int forgeLevel = upgrades != null ? upgrades.getForge() : 0;
                if (forgeLevel <= 0) continue;

                long diamondInterval = forgeDiamondIntervalSeconds(forgeLevel);
                if (diamondInterval > 0) {
                    long count = forgeDiamondCounter.merge(color, 1L, Long::sum);
                    if (count >= diamondInterval) {
                        forgeDiamondCounter.put(color, 0L);
                        dropForgeBonus(team.getForgeLocation(), Material.DIAMOND);
                    }
                }
                long emeraldInterval = forgeEmeraldIntervalSeconds(forgeLevel);
                if (emeraldInterval > 0) {
                    long count = forgeEmeraldCounter.merge(color, 1L, Long::sum);
                    if (count >= emeraldInterval) {
                        forgeEmeraldCounter.put(color, 0L);
                        dropForgeBonus(team.getForgeLocation(), Material.EMERALD);
                    }
                }
            }
        }, 20L, 20L); // vérifié toutes les secondes
    }

    private long forgeDiamondIntervalSeconds(int forgeLevel) {
        if (forgeLevel >= 4) return 15;
        if (forgeLevel == 3) return 60;
        return 0;
    }

    private long forgeEmeraldIntervalSeconds(int forgeLevel) {
        return forgeLevel >= 4 ? 120 : 0;
    }

    private void dropForgeBonus(Location loc, Material material) {
        loc.getWorld().dropItem(loc.clone().add(0.5, 0.2, 0.5), new ItemStack(material, 1));
    }

    private void schedulePreciousGenerator(Generator gen) {
        BukkitTask existing = preciousTasks.get(gen);
        if (existing != null) existing.cancel();

        long intervalSeconds = getIntervalFor(gen.getType());
        long periodTicks = intervalSeconds * 20L;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isGameActive()) return;
            spawnCappedResource(gen);
        }, periodTicks, periodTicks);
        preciousTasks.put(gen, task);
    }

    private long getIntervalFor(GeneratorType type) {
        String prefix = type == GeneratorType.DIAMOND ? "diamond" : "emerald";
        String key = switch (phase) {
            case 2 -> "generators." + prefix + "-interval-seconds-phase2";
            case 3 -> "generators." + prefix + "-interval-seconds-phase3";
            default -> "generators." + prefix + "-interval-seconds";
        };
        return plugin.getConfig().getLong(key, type == GeneratorType.DIAMOND ? 30 : 45);
    }

    /** Fer/Or : se répartit automatiquement entre les joueurs présents sur le générateur. */
    private void spawnSplitResource(Generator gen) {
        List<Player> nearby = new ArrayList<>();
        for (UUID uuid : getAllParticipantIds()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !isAlivePlaying(p)) continue;
            if (p.getLocation().getWorld().equals(gen.getLocation().getWorld())
                    && p.getLocation().distanceSquared(gen.getLocation()) <= 4.0 * 4.0) {
                nearby.add(p);
            }
        }
        if (nearby.isEmpty()) {
            gen.getLocation().getWorld().dropItem(gen.getLocation().clone().add(0.5, 0.2, 0.5),
                    new ItemStack(gen.getType().getMaterial(), 1));
        } else {
            for (Player p : nearby) {
                giveOrDrop(p, new ItemStack(gen.getType().getMaterial(), 1));
            }
        }
    }

    /** Diamant/Émeraude : toujours au sol, jamais partagé automatiquement, avec un plafond au sol. */
    private void spawnCappedResource(Generator gen) {
        List<UUID> alive = groundItems.computeIfAbsent(gen, g -> new ArrayList<>());
        alive.removeIf(id -> {
            org.bukkit.entity.Entity e = Bukkit.getEntity(id);
            return e == null || e.isDead() || !e.isValid();
        });
        int cap = gen.getType() == GeneratorType.DIAMOND
                ? plugin.getConfig().getInt("generators.diamond-max-per-spawner", 8)
                : plugin.getConfig().getInt("generators.emerald-max-per-spawner", 4);
        if (alive.size() >= cap) return;

        Item item = gen.getLocation().getWorld().dropItem(gen.getLocation().clone().add(0.5, 0.2, 0.5),
                new ItemStack(gen.getType().getMaterial(), 1));
        alive.add(item.getUniqueId());
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), extra);
        }
    }

    // ---------------------------------------------------------------
    // Timer principal / mort subite
    // ---------------------------------------------------------------

    /** Libellé de la prochaine phase et temps restant avant qu'elle démarre (pour le scoreboard). */
    public String getPhaseCountdownLabel() {
        if (arena.getState() == ArenaState.SUDDEN_DEATH) return "Mort subite en cours";

        int durationMinutes = plugin.getConfig().getInt("game.game-duration-minutes", 45);
        int phase2Minute = plugin.getConfig().getInt("generators.phase2-minute", 25);
        int phase3Minute = plugin.getConfig().getInt("generators.phase3-minute", 5);
        int totalSeconds = durationMinutes * 60;
        int remainingSeconds = Math.max(0, totalSeconds - elapsedSeconds);

        String label;
        int targetMinuteMark;
        if (phase == 1) {
            label = "Phase 2";
            targetMinuteMark = phase2Minute;
        } else if (phase == 2) {
            label = "Phase 3";
            targetMinuteMark = phase3Minute;
        } else {
            label = "Mort subite";
            targetMinuteMark = 0;
        }

        int secondsUntil = Math.max(0, remainingSeconds - targetMinuteMark * 60);
        return label + ": " + formatTime(secondsUntil);
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void startMainTimer() {
        int durationMinutes = plugin.getConfig().getInt("game.game-duration-minutes", 45);
        int phase2Minute = plugin.getConfig().getInt("generators.phase2-minute", 25);
        int phase3Minute = plugin.getConfig().getInt("generators.phase3-minute", 5);
        int totalSeconds = durationMinutes * 60;

        mainTimerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            elapsedSeconds++;
            int remainingSeconds = totalSeconds - elapsedSeconds;
            int remainingMinutes = remainingSeconds / 60;

            if (phase == 1 && remainingMinutes <= phase2Minute) {
                phase = 2;
                broadcastToArena(ChatColor.GOLD + "Les diamants et émeraudes apparaissent plus vite !");
                rescheduleAllPrecious();
            } else if (phase == 2 && remainingMinutes <= phase3Minute) {
                phase = 3;
                broadcastToArena(ChatColor.GOLD + "" + ChatColor.BOLD + "Les diamants et émeraudes apparaissent encore plus vite !");
                rescheduleAllPrecious();
            }

            if (remainingSeconds <= 0 && !suddenDeathTriggered) {
                triggerSuddenDeath();
            }
        }, 20L, 20L);
    }

    private void rescheduleAllPrecious() {
        for (Generator gen : arena.getGenerators()) {
            if (gen.getType() == GeneratorType.DIAMOND || gen.getType() == GeneratorType.EMERAUDE) {
                schedulePreciousGenerator(gen);
            }
        }
    }

    private void triggerSuddenDeath() {
        suddenDeathTriggered = true;
        arena.setState(ArenaState.SUDDEN_DEATH);
        broadcastToArena(ChatColor.DARK_RED + "" + ChatColor.BOLD + "MORT SUBITE ! Des dragons apparaissent !");

        int height = plugin.getConfig().getInt("game.sudden-death-dragon-height-above-bed", 20);
        List<EnderDragon> dragons = new ArrayList<>();
        for (TeamColor color : TeamColor.forTeamCount(arena.getTeamCount())) {
            ArenaTeam team = arena.getTeams().get(color);
            if (team == null || team.getBedLocation() == null) continue;
            if (team.isEliminated()) continue;
            Location spawnLoc = team.getBedLocation().clone().add(0, height, 0);
            dragons.add(spawnDragonFor(color, spawnLoc));

            // "Dragon Buff" (Amélioration d'équipe) : un second dragon, uniquement à la mort
            // subite (une fois toutes les phases/le compteur terminés), pas avant.
            TeamUpgrades upgrades = teamUpgrades.get(color);
            if (upgrades != null && upgrades.isDragonBuff()) {
                Location secondSpawnLoc = spawnLoc.clone().add(4, 2, 4);
                dragons.add(spawnDragonFor(color, secondSpawnLoc));
            }
        }

        // Tâche périodique : chaque dragon vise un joueur d'une autre équipe.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (arena.getState() != ArenaState.SUDDEN_DEATH) return;
            for (EnderDragon dragon : dragons) {
                if (dragon.isDead()) continue;
                TeamColor dragonTeam = teamColorFromDragonName(dragon.getCustomName());
                Player target = findEnemyPlayer(dragonTeam);
                if (target != null && dragon instanceof Mob mob) {
                    mob.setTarget(target);
                }
            }
        }, 0L, 60L);
    }

    private TeamColor teamColorFromDragonName(String name) {
        if (name == null) return null;
        for (TeamColor color : TeamColor.values()) {
            if (name.startsWith(color.getColoredName())) return color;
        }
        return null;
    }

    private Player findEnemyPlayer(TeamColor excluding) {
        List<Player> candidates = new ArrayList<>();
        for (Map.Entry<UUID, TeamColor> entry : playerTeams.entrySet()) {
            if (entry.getValue() == excluding) continue;
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null && isAlivePlaying(p)) candidates.add(p);
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(new Random().nextInt(candidates.size()));
    }

    // ---------------------------------------------------------------
    // Combat / mort / lits (appelés par les listeners)
    // ---------------------------------------------------------------

    public boolean isAlivePlaying(Player player) {
        TeamColor color = playerTeams.get(player.getUniqueId());
        if (color == null) return false;
        ArenaTeam team = arena.getTeams().get(color);
        return team != null && team.getAlivePlayers().contains(player.getUniqueId());
    }

    public TeamColor getTeamColor(Player player) {
        return playerTeams.get(player.getUniqueId());
    }

    public boolean isSameTeam(Player a, Player b) {
        TeamColor ca = playerTeams.get(a.getUniqueId());
        TeamColor cb = playerTeams.get(b.getUniqueId());
        return ca != null && ca == cb;
    }

    public void onBedBroken(TeamColor color, Player breaker) {
        ArenaTeam team = arena.getTeams().get(color);
        if (team == null || team.isBedDestroyed()) return;
        team.setBedDestroyed(true);

        String breakerName = breaker != null ? breaker.getName() : "quelqu'un";
        broadcastToArena(ChatColor.RED + "" + ChatColor.BOLD + "LIT DÉTRUIT ! " + ChatColor.RESET
                + color.getColoredName() + ChatColor.GRAY + " a perdu son lit, détruit par " + ChatColor.YELLOW + breakerName);

        for (UUID uuid : team.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "LIT DÉTRUIT !", ChatColor.GRAY + "Vous ne pouvez plus respawn.", 5, 60, 10);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1f, 1f);
        }
    }

    public void onPlayerFinalDeath(Player victim) {
        TeamColor color = playerTeams.get(victim.getUniqueId());
        if (color == null) return;
        ArenaTeam team = arena.getTeams().get(color);
        if (team == null) return;
        team.getAlivePlayers().remove(victim.getUniqueId());

        toSpectator(victim);

        if (team.isEliminated()) {
            broadcastToArena(color.getColoredName() + ChatColor.GRAY + " a été éliminée !");
        }

        checkGameEnd();
    }

    public void respawnAtTeamSpawn(Player player) {
        TeamColor color = playerTeams.get(player.getUniqueId());
        ArenaTeam team = color != null ? arena.getTeams().get(color) : null;
        if (team != null && team.getSpawnLocation() != null) {
            player.teleport(team.getSpawnLocation());
        }
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        if (color != null) {
            downgradeTools(player.getUniqueId());
            giveKit(player, color);
        }
    }

    /** Fait apparaître un dragon nommé d'après son équipe (utilisé par triggerSuddenDeath). */
    private EnderDragon spawnDragonFor(TeamColor color, Location spawnLoc) {
        EnderDragon dragon = (EnderDragon) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ENDER_DRAGON);
        dragon.setCustomName(color.getColoredName() + " Dragon");
        dragon.setCustomNameVisible(true);
        return dragon;
    }

    public void toSpectator(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        if (arena.getSpecLocation() != null) {
            player.teleport(arena.getSpecLocation());
        }
        if (!arena.getSpectators().contains(player.getUniqueId())) {
            arena.getSpectators().add(player.getUniqueId());
        }
    }

    private void checkGameEnd() {
        List<TeamColor> remaining = new ArrayList<>();
        for (TeamColor color : TeamColor.forTeamCount(arena.getTeamCount())) {
            ArenaTeam team = arena.getTeams().get(color);
            if (team == null) continue;
            if (!team.isEliminated()) remaining.add(color);
        }
        if (remaining.size() <= 1) {
            TeamColor winner = remaining.isEmpty() ? null : remaining.get(0);
            endGame(winner);
        }
    }

    private void endGame(TeamColor winner) {
        arena.setState(ArenaState.ENDING);
        cancelAllTasks();

        if (winner != null) {
            broadcastToArena(ChatColor.GOLD + "" + ChatColor.BOLD + "Victoire de l'équipe " + winner.getColoredName() + ChatColor.GOLD + " !");
            launchFireworks(winner);
        } else {
            broadcastToArena(ChatColor.GRAY + "Partie terminée, aucune équipe survivante.");
        }

        int delay = plugin.getConfig().getInt("game.end-reset-delay-seconds", 10);
        Bukkit.getScheduler().runTaskLater(plugin, this::resetArena, delay * 20L);
    }

    private void launchFireworks(TeamColor winner) {
        ArenaTeam team = arena.getTeams().get(winner);
        if (team == null || team.getSpawnLocation() == null) return;
        Location loc = team.getSpawnLocation();
        for (int i = 0; i < 5; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                org.bukkit.entity.Firework fw = (org.bukkit.entity.Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
                org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(org.bukkit.FireworkEffect.builder()
                        .withColor(org.bukkit.Color.fromRGB(winner.getArmorColor().asRGB()))
                        .with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
                        .withFlicker().withTrail().build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
            }, i * 10L);
        }
    }

    private void resetArena() {
        for (UUID uuid : getAllParticipantIds()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            plugin.getScoreboardManager().clear(p);
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            if (arena.getSpecLocation() != null) {
                p.teleport(arena.getSpecLocation());
            }
        }
        plugin.getArenaManager().restoreRegion(arena);
        arena.resetRuntime();
        playerTeams.clear();
        kills.clear();
        finalKills.clear();
        groundItems.clear();
        pickaxeTier.clear();
        axeTier.clear();
        swordTier.clear();
        preferredTeam.clear();
        elapsedSeconds = 0;
        phase = 1;
        suddenDeathTriggered = false;
        plugin.getGameManager().replaceInstance(arena);
    }

    private void cancelAllTasks() {
        if (lobbyCountdownTask != null) lobbyCountdownTask.cancel();
        if (mainTimerTask != null) mainTimerTask.cancel();
        if (ironGoldTask != null) ironGoldTask.cancel();
        if (scoreboardTask != null) scoreboardTask.cancel();
        if (upgradeEffectsTask != null) upgradeEffectsTask.cancel();
        if (forgeBonusTask != null) forgeBonusTask.cancel();
        for (BukkitTask task : preciousTasks.values()) task.cancel();
    }

    // ---------------------------------------------------------------
    // Divers
    // ---------------------------------------------------------------

    public void trackPlacedBlock(Location loc) {
        placedBlocks.add(loc.getBlock().getLocation());
    }

    public boolean isPlacedBlock(Location loc) {
        return placedBlocks.contains(loc.getBlock().getLocation());
    }

    public void untrackBlock(Location loc) {
        placedBlocks.remove(loc.getBlock().getLocation());
    }

    public int getKills(UUID uuid) {
        return kills.getOrDefault(uuid, 0);
    }

    public int getFinalKills(UUID uuid) {
        return finalKills.getOrDefault(uuid, 0);
    }

    public void addKill(UUID uuid) {
        kills.merge(uuid, 1, Integer::sum);
    }

    public void addFinalKill(UUID uuid) {
        finalKills.merge(uuid, 1, Integer::sum);
    }

    public List<Player> getAllParticipants() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : getAllParticipantIds()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) players.add(p);
        }
        return players;
    }

    private Set<UUID> getAllParticipantIds() {
        Set<UUID> all = new HashSet<>(arena.getWaitingPlayers());
        all.addAll(playerTeams.keySet());
        all.addAll(arena.getSpectators());
        return all;
    }

    private void broadcastToArena(String message) {
        for (Player p : getAllParticipants()) {
            p.sendMessage(message);
        }
    }
}
