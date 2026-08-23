package com.bedwars;

import com.bedwars.arena.ArenaManager;
import com.bedwars.commands.BedwarsCommand;
import com.bedwars.game.GameManager;
import com.bedwars.game.WaitingLobbyManager;
import com.bedwars.gui.AdminGUIManager;
import com.bedwars.gui.AdminNPCManager;
import com.bedwars.listeners.CombatListener;
import com.bedwars.listeners.GUIListener;
import com.bedwars.listeners.KitProtectionListener;
import com.bedwars.listeners.LobbyItemListener;
import com.bedwars.listeners.OreMergeListener;
import com.bedwars.listeners.PlayerProtectionListener;
import com.bedwars.listeners.ShopListener;
import com.bedwars.npc.ShopNpcManager;
import com.bedwars.scoreboard.ScoreboardManager;
import com.bedwars.shop.ShopConfigManager;
import com.bedwars.shop.ShopGUIManager;
import com.bedwars.upgrade.UpgradeGUIManager;
import com.bedwars.util.KitProtectionUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class BedwarsPlugin extends JavaPlugin {

    private static BedwarsPlugin instance;

    private ArenaManager arenaManager;
    private GameManager gameManager;
    private ScoreboardManager scoreboardManager;
    private AdminNPCManager adminNPCManager;
    private AdminGUIManager adminGUIManager;
    private WaitingLobbyManager waitingLobbyManager;
    private ShopNpcManager shopNpcManager;
    private ShopGUIManager shopGUIManager;
    private ShopConfigManager shopConfigManager;
    private UpgradeGUIManager upgradeGUIManager;
    private com.bedwars.util.PlayerPrefsManager playerPrefsManager;
    private com.bedwars.stats.StatsManager statsManager;
    private com.bedwars.stats.LeaderboardManager leaderboardManager;
    private com.bedwars.util.FavoritesManager favoritesManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        KitProtectionUtil.init(this);
        com.bedwars.util.LobbyItemUtil.init(this);
        ShopNpcManager.init(this);

        this.arenaManager = new ArenaManager(this);
        this.scoreboardManager = new ScoreboardManager();
        this.gameManager = new GameManager(this);
        this.adminNPCManager = new AdminNPCManager(this);
        this.adminGUIManager = new AdminGUIManager(this);
        this.waitingLobbyManager = new WaitingLobbyManager(this);
        this.shopNpcManager = new ShopNpcManager(this);
        this.shopConfigManager = new ShopConfigManager(this);
        this.shopGUIManager = new ShopGUIManager(this);
        this.upgradeGUIManager = new UpgradeGUIManager(this);
        this.playerPrefsManager = new com.bedwars.util.PlayerPrefsManager(this);
        this.statsManager = new com.bedwars.stats.StatsManager(this);
        this.leaderboardManager = new com.bedwars.stats.LeaderboardManager(this);
        this.favoritesManager = new com.bedwars.util.FavoritesManager(this);

        arenaManager.loadAll();
        // Nettoyage définitif de l'ancien PNJ hub (fonctionnalité remplacée par /bd arene gui) :
        // supprime tout PNJ résiduel encore présent dans le monde, une fois pour toutes.
        adminNPCManager.purgeLegacyNpc();
        shopConfigManager.load();
        shopNpcManager.spawnAll();
        playerPrefsManager.load();
        statsManager.load();
        leaderboardManager.load();
        favoritesManager.load();

        BedwarsCommand command = new BedwarsCommand(this);
        getCommand("bd").setExecutor(command);
        getCommand("bd").setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new OreMergeListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        getServer().getPluginManager().registerEvents(new KitProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyItemListener(this), this);
        getServer().getPluginManager().registerEvents(new com.bedwars.listeners.TntListener(this), this);
        getServer().getPluginManager().registerEvents(new com.bedwars.listeners.FireballListener(this), this);

        getLogger().info("BedwarsPlugin activé — " + arenaManager.getArenas().size() + " arène(s) chargée(s).");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdownAll();
        }
        if (adminNPCManager != null) {
            adminNPCManager.removeNPC();
        }
        if (shopNpcManager != null) {
            shopNpcManager.removeAll();
        }
    }

    public static BedwarsPlugin getInstance() {
        return instance;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public AdminNPCManager getAdminNPCManager() {
        return adminNPCManager;
    }

    public AdminGUIManager getAdminGUIManager() {
        return adminGUIManager;
    }

    public WaitingLobbyManager getWaitingLobbyManager() {
        return waitingLobbyManager;
    }

    public ShopNpcManager getShopNpcManager() {
        return shopNpcManager;
    }

    public ShopGUIManager getShopGUIManager() {
        return shopGUIManager;
    }

    public ShopConfigManager getShopConfigManager() {
        return shopConfigManager;
    }

    public UpgradeGUIManager getUpgradeGUIManager() {
        return upgradeGUIManager;
    }

    public com.bedwars.util.PlayerPrefsManager getPlayerPrefsManager() {
        return playerPrefsManager;
    }

    public com.bedwars.stats.StatsManager getStatsManager() {
        return statsManager;
    }

    public com.bedwars.stats.LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public com.bedwars.util.FavoritesManager getFavoritesManager() {
        return favoritesManager;
    }
}
