package com.bedwars.game;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.*;
import com.bedwars.shop.ShopConfig;
import com.bedwars.shop.UpgradeType;
import com.bedwars.shop.UpgradeTypeConfig;
import com.bedwars.util.LobbyFloorManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
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
    private final Map<Generator, TeamColor> generatorOwners = new HashMap<>();
    private final Map<TeamColor, Long> forgeEmeraldLastSpawn = new EnumMap<>(TeamColor.class);
    private List<Block> lobbyFloorBlocks = new ArrayList<>();

    private BukkitTask lobbyCountdownTask;
    private int lobbyCountdown;

    private BukkitTask mainTimerTask;
    private BukkitTask ironGoldTask;
    private BukkitTask upgradeTask;
    private final Map<Generator, BukkitTask> preciousTasks = new HashMap<>();
    private BukkitTask scoreboardTask;

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

    /** Permet de transférer un sol de lobby déjà construit à une nouvelle instance (voir GameManager). */
    public void setInitialLobbyFloor(List<Block> blocks) {
        this.lobbyFloorBlocks = blocks != null ? blocks : new ArrayList<>();
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
        if (wasEmpty) {
            ensureLobbyFloor();
        }
        Location lobbyCenter = arena.computeLobbyPlatformCenter(lobbyHeightAboveMap());
        if (lobbyCenter != null) {
            player.teleport(lobbyCenter.clone().add(0.5, 1.0, 0.5));
        }
        broadcastToArena(ChatColor.YELLOW + player.getName() + ChatColor.GRAY + " a rejoint la partie ("
                + arena.getCurrentPlayerCount() + "/" + arena.getMaxPlayers() + ")");
        if (arena.isLobbyFull() && arena.getState() == ArenaState.WAITING) {
            startCountdown();
        }
        return true;
    }

    private int lobbyHeightAboveMap() {
        return plugin.getConfig().getInt("lobby.height-above-map", 15);
    }

    private int lobbyPlatformRadius() {
        return plugin.getConfig().getInt("lobby.platform-radius", 4);
    }

    /** Fait apparaître temporairement la plateforme de lobby au-dessus de la map. */
    private void ensureLobbyFloor() {
        if (!lobbyFloorBlocks.isEmpty()) return;
        Location center = arena.computeLobbyPlatformCenter(lobbyHeightAboveMap());
        if (center == null) return;
        lobbyFloorBlocks = LobbyFloorManager.build(center, lobbyPlatformRadius());
    }

    public void removeWaitingPlayer(Player player) {
        arena.getWaitingPlayers().remove(player.getUniqueId());
        if (arena.getState() == ArenaState.STARTING && !arena.isLobbyFull()) {
            cancelCountdown();
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
        assignTeams();
        LobbyFloorManager.remove(lobbyFloorBlocks);
        lobbyFloorBlocks = new ArrayList<>();
        if (arena.getSpecLocation() != null) {
            com.bedwars.util.SpectatorCageManager.build(plugin, arena.getSpecLocation());
        }

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

        computeGeneratorOwners();
        startGenerators();
        startMainTimer();
        startUpgradeTask();
        scoreboardTask = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> plugin.getScoreboardManager().update(this), 0L, 20L);

        broadcastToArena(ChatColor.GREEN + "" + ChatColor.BOLD + "La partie commence !");
    }

    /** Détermine, pour chaque générateur fer/or, l'équipe la plus proche (pour la Forge). */
    private void computeGeneratorOwners() {
        generatorOwners.clear();
        for (Generator gen : arena.getGenerators()) {
            if (gen.getType() != GeneratorType.FER && gen.getType() != GeneratorType.OR) continue;
            TeamColor closest = null;
            double bestDistSq = 40.0 * 40.0;
            for (TeamColor color : TeamColor.forTeamCount(arena.getTeamCount())) {
                ArenaTeam team = arena.getTeams().get(color);
                if (team == null || team.getBedLocation() == null) continue;
                if (!team.getBedLocation().getWorld().equals(gen.getLocation().getWorld())) continue;
                double distSq = team.getBedLocation().distanceSquared(gen.getLocation());
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    closest = color;
                }
            }
            if (closest != null) generatorOwners.put(gen, closest);
        }
    }

    private void assignTeams() {
        TeamColor[] colors = TeamColor.forTeamCount(arena.getTeamCount());
        List<UUID> players = new ArrayList<>(arena.getWaitingPlayers());
        Collections.shuffle(players);

        int index = 0;
        for (UUID uuid : players) {
            TeamColor color = colors[index % colors.length];
            ArenaTeam team = arena.getOrCreateTeam(color);
            team.getMembers().add(uuid);
            team.getAlivePlayers().add(uuid);
            playerTeams.put(uuid, color);
            index++;
        }
    }

    private void giveKit(Player player, TeamColor color) {
        ArenaTeam team = arena.getTeams().get(color);
        int armorLevel = team != null ? team.getUpgradeLevel(UpgradeType.ARMOR) : 1;
        int protectionLevel = armorLevel - 1; // niveau 1 = aucun bonus (comme sur Hypixel)
        player.getInventory().setHelmet(enchantedIfNeeded(dyed(Material.LEATHER_HELMET, color), protectionLevel));
        player.getInventory().setChestplate(enchantedIfNeeded(dyed(Material.LEATHER_CHESTPLATE, color), protectionLevel));
        player.getInventory().setLeggings(enchantedIfNeeded(dyed(Material.LEATHER_LEGGINGS, color), protectionLevel));
        player.getInventory().setBoots(enchantedIfNeeded(dyed(Material.LEATHER_BOOTS, color), protectionLevel));
        player.getInventory().addItem(new ItemStack(Material.WOODEN_SWORD));
    }

    private ItemStack enchantedIfNeeded(ItemStack item, int protectionLevel) {
        if (protectionLevel > 0) {
            item.addUnsafeEnchantment(Enchantment.PROTECTION, protectionLevel);
        }
        return item;
    }

    /** Ré-applique l'enchantement Protection sur l'armure déjà portée (après achat de l'upgrade Armure). */
    public void reapplyArmorEnchant(ArenaTeam team) {
        int protectionLevel = team.getUpgradeLevel(UpgradeType.ARMOR) - 1; // niveau 1 = aucun bonus
        for (UUID uuid : team.getAlivePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            for (ItemStack piece : new ItemStack[]{p.getInventory().getHelmet(), p.getInventory().getChestplate(),
                    p.getInventory().getLeggings(), p.getInventory().getBoots()}) {
                if (piece == null) continue;
                if (protectionLevel > 0) {
                    piece.addUnsafeEnchantment(Enchantment.PROTECTION, protectionLevel);
                } else {
                    piece.removeEnchantment(Enchantment.PROTECTION);
                }
            }
        }
    }

    private ItemStack dyed(Material material, TeamColor color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color.getArmorColor());
        item.setItemMeta(meta);
        return item;
    }

    // ---------------------------------------------------------------
    // Générateurs de ressources
    // ---------------------------------------------------------------

    private void startGenerators() {
        ironGoldTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Generator gen : arena.getGenerators()) {
                if (gen.getType() != GeneratorType.FER && gen.getType() != GeneratorType.OR) continue;
                gen.incrementTick();
                long interval = forgeAdjustedInterval(gen);
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

    private long forgeAdjustedInterval(Generator gen) {
        TeamColor owner = generatorOwners.get(gen);
        int forgeLevel = 1;
        if (owner != null) {
            ArenaTeam team = arena.getTeams().get(owner);
            if (team != null) forgeLevel = team.getUpgradeLevel(UpgradeType.FORGE);
        }
        if (gen.getType() == GeneratorType.FER) {
            return switch (forgeLevel) {
                case 1 -> plugin.getConfig().getLong("generators.iron-interval-ticks", 20);
                case 2 -> 15L;
                case 3 -> 10L;
                default -> 5L; // niveau 4 et 5
            };
        } else {
            return switch (forgeLevel) {
                case 1 -> plugin.getConfig().getLong("generators.gold-interval-ticks", 80);
                case 2 -> 60L;
                case 3 -> 45L;
                case 4 -> 30L;
                default -> 20L; // niveau 5
            };
        }
    }

    private void schedulePreciousGenerator(Generator gen) {
        BukkitTask existing = preciousTasks.get(gen);
        if (existing != null) existing.cancel();

        long intervalSeconds = getIntervalFor(gen.getType());
        long periodTicks = intervalSeconds * 20L;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> spawnCappedResource(gen),
                periodTicks, periodTicks);
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

            if (team.getUpgradeLevel(UpgradeType.DRAGON) >= 1) {
                Location secondSpawnLoc = team.getBedLocation().clone().add(3, height + 3, 3);
                dragons.add(spawnDragonFor(color, secondSpawnLoc));
                broadcastToArena(color.getColoredName() + ChatColor.GRAY + " bénéficie d'un second dragon (Dragon Buff) !");
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

    private EnderDragon spawnDragonFor(TeamColor color, Location spawnLoc) {
        EnderDragon dragon = (EnderDragon) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ENDER_DRAGON);
        dragon.setCustomName(color.getColoredName() + " Dragon");
        dragon.setCustomNameVisible(true);
        return dragon;
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
    // Achat et effets des améliorations d'équipe (villageois Upgrade)
    // ---------------------------------------------------------------

    /**
     * Achète/améliore une upgrade pour l'équipe du joueur. Les upgrades à palier
     * (Sharp, Armure, Maniac, Chute, Forge) démarrent toutes au niveau 1 (le niveau
     * de base, sans bonus particulier sauf pour la Forge) et progressent d'un niveau
     * à chaque achat. Heal Boost et Dragon Buff sont des achats uniques. Les pièges
     * (TrapA/TrapC/TrapM) sont à usage unique : il faut les racheter après déclenchement.
     */
    public void purchaseUpgrade(Player player, ArenaTeam team, UpgradeType type) {
        ShopConfig config = plugin.getShopConfigManager().getConfig();
        UpgradeTypeConfig cfg = config.getUpgradeConfig(type);

        if (type.isTrap()) {
            if (team.isTrapArmed(type)) {
                player.sendMessage(ChatColor.YELLOW + "Ce piège est déjà armé.");
                return;
            }
            int level = team.getUpgradeLevel(type);
            int price = cfg.getPriceForLevel(level);
            if (!chargeDiamonds(player, price)) return;
            team.setTrapArmed(type, true);
            broadcastToTeam(team, ChatColor.GREEN + "Piège " + type.getLabel() + " armé (niveau " + level + ").");
            return;
        }

        if (!type.isLeveled()) {
            if (team.getUpgradeLevel(type) >= 1) {
                player.sendMessage(ChatColor.YELLOW + type.getLabel() + " est déjà actif pour votre équipe.");
                return;
            }
            int price = cfg.getPriceForLevel(1);
            if (!chargeDiamonds(player, price)) return;
            team.setUpgradeLevel(type, 1);
            broadcastToTeam(team, ChatColor.GREEN + type.getLabel() + " activé pour l'équipe !");
            return;
        }

        int currentLevel = team.getUpgradeLevel(type);
        int max = cfg.getMaxLevel();
        if (currentLevel >= max) {
            player.sendMessage(ChatColor.YELLOW + type.getLabel() + " est déjà au niveau maximum.");
            return;
        }
        int nextLevel = currentLevel + 1;
        int price = cfg.getPriceForLevel(nextLevel);
        if (!chargeDiamonds(player, price)) return;
        team.setUpgradeLevel(type, nextLevel);
        broadcastToTeam(team, ChatColor.GREEN + type.getLabel() + " amélioré au niveau " + nextLevel + " !");
        if (type == UpgradeType.ARMOR) reapplyArmorEnchant(team);
    }

    private boolean chargeDiamonds(Player player, int price) {
        int have = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DIAMOND) have += item.getAmount();
        }
        if (have < price) {
            player.sendMessage(ChatColor.RED + "Il vous manque " + (price - have) + " diamant(s) pour cet achat.");
            return false;
        }
        int remaining = price;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != Material.DIAMOND) continue;
            int take = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - take);
            remaining -= take;
            if (item.getAmount() <= 0) player.getInventory().setItem(i, null);
        }
        return true;
    }

    private void broadcastToTeam(ArenaTeam team, String message) {
        for (UUID uuid : team.getMembers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    /** Tâche périodique (1x/seconde) : auras Sharp/Maniac/Heal Boost, pièges, bonus Forge. */
    private void startUpgradeTask() {
        upgradeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (TeamColor color : TeamColor.forTeamCount(arena.getTeamCount())) {
                ArenaTeam team = arena.getTeams().get(color);
                if (team == null || team.isEliminated()) continue;
                applyAuraEffects(team);
                checkForgeEmeraldBonus(color, team);
                checkTraps(color, team);
            }
        }, 40L, 20L);
    }

    private void applyAuraEffects(ArenaTeam team) {
        int sharpLevel = team.getUpgradeLevel(UpgradeType.SHARP);
        int maniacLevel = team.getUpgradeLevel(UpgradeType.MANIAC);
        boolean healBoost = team.getUpgradeLevel(UpgradeType.HEAL) >= 1;

        for (UUID uuid : team.getAlivePlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            if (sharpLevel >= 2) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 60, sharpLevel - 2, true, false));
            }
            if (maniacLevel >= 2) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 60, maniacLevel - 2, true, false));
            }
            if (healBoost && team.getBedLocation() != null
                    && p.getWorld().equals(team.getBedLocation().getWorld())
                    && p.getLocation().distanceSquared(team.getBedLocation()) <= 20.0 * 20.0) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 0, true, false));
            }
        }
    }

    /** Fraction de dégâts de chute conservée (0 = plus aucun dégât) selon le niveau "Chute" (niveau 1 = aucun bonus). */
    public double getFallDamageMultiplier(Player player) {
        TeamColor color = playerTeams.get(player.getUniqueId());
        if (color == null) return 1.0;
        ArenaTeam team = arena.getTeams().get(color);
        if (team == null) return 1.0;
        UpgradeTypeConfig cfg = plugin.getShopConfigManager().getConfig().getUpgradeConfig(UpgradeType.CHUTE);
        int level = team.getUpgradeLevel(UpgradeType.CHUTE);
        int max = Math.max(cfg.getMaxLevel(), 2);
        double reduction = Math.min(1.0, Math.max(0.0, (double) (level - 1) / (max - 1)));
        return 1.0 - reduction;
    }

    private void checkForgeEmeraldBonus(TeamColor color, ArenaTeam team) {
        int forgeLevel = team.getUpgradeLevel(UpgradeType.FORGE);
        if (forgeLevel < 4 || team.getBedLocation() == null) return;
        long intervalSeconds = forgeLevel >= 5 ? 30L : 45L;
        long now = elapsedSeconds;
        long last = forgeEmeraldLastSpawn.getOrDefault(color, -intervalSeconds);
        if (now - last < intervalSeconds) return;
        forgeEmeraldLastSpawn.put(color, now);
        team.getBedLocation().getWorld().dropItem(team.getBedLocation().clone().add(0.5, 1.2, 0.5),
                new ItemStack(Material.EMERALD, 1));
    }

    private void checkTraps(TeamColor color, ArenaTeam team) {
        if (team.getBedLocation() == null) return;
        for (UpgradeType trap : new UpgradeType[]{UpgradeType.TRAP_ALARM, UpgradeType.TRAP_BLIND, UpgradeType.TRAP_MINER}) {
            if (!team.isTrapArmed(trap)) continue;
            int level = team.getUpgradeLevel(trap);
            double radius = level >= 2 ? 20.0 : 10.0;
            List<Player> enemies = findEnemiesNear(color, team.getBedLocation(), radius);
            if (enemies.isEmpty()) continue;

            team.setTrapArmed(trap, false);
            switch (trap) {
                case TRAP_ALARM -> broadcastToTeam(team, ChatColor.RED + "" + ChatColor.BOLD
                        + "ALARME ! " + ChatColor.RESET + ChatColor.GRAY + "Un ennemi approche de votre lit !");
                case TRAP_BLIND -> {
                    for (Player enemy : enemies) {
                        enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                    }
                    broadcastToTeam(team, ChatColor.GRAY + "Le piège de cécité s'est déclenché !");
                }
                case TRAP_MINER -> {
                    int amplifier = level >= 2 ? 2 : 1;
                    for (Player enemy : enemies) {
                        enemy.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 160, amplifier));
                    }
                    broadcastToTeam(team, ChatColor.GRAY + "Le piège du mineur s'est déclenché !");
                }
                default -> {}
            }
        }
    }

    private List<Player> findEnemiesNear(TeamColor ownTeam, Location center, double radius) {
        List<Player> result = new ArrayList<>();
        for (Map.Entry<UUID, TeamColor> entry : playerTeams.entrySet()) {
            if (entry.getValue() == ownTeam) continue;
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null || !isAlivePlaying(p)) continue;
            if (!p.getWorld().equals(center.getWorld())) continue;
            if (p.getLocation().distanceSquared(center) <= radius * radius) result.add(p);
        }
        return result;
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
        if (color != null) giveKit(player, color);
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
        Location lobbyCenter = arena.computeLobbyPlatformCenter(lobbyHeightAboveMap());
        if (lobbyCenter != null && getAllParticipants().size() > 0) {
            lobbyFloorBlocks = LobbyFloorManager.build(lobbyCenter, lobbyPlatformRadius());
        }
        for (UUID uuid : getAllParticipantIds()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            plugin.getScoreboardManager().clear(p);
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            if (lobbyCenter != null) {
                p.teleport(lobbyCenter.clone().add(0.5, 1.0, 0.5));
            }
        }
        plugin.getArenaManager().restoreRegion(arena);
        arena.resetRuntime();
        playerTeams.clear();
        kills.clear();
        finalKills.clear();
        groundItems.clear();
        elapsedSeconds = 0;
        phase = 1;
        suddenDeathTriggered = false;
        plugin.getGameManager().replaceInstance(arena, lobbyFloorBlocks);
    }

    private void cancelAllTasks() {
        if (lobbyCountdownTask != null) lobbyCountdownTask.cancel();
        if (mainTimerTask != null) mainTimerTask.cancel();
        if (ironGoldTask != null) ironGoldTask.cancel();
        if (upgradeTask != null) upgradeTask.cancel();
        if (scoreboardTask != null) scoreboardTask.cancel();
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
