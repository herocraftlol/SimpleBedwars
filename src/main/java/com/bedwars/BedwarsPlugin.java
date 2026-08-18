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

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        KitProtectionUtil.init(this);
        com.bedwars.util.LobbyItemUtil.init(this);

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

        arenaManager.loadAll();
        adminNPCManager.load();
        shopConfigManager.load();
        shopNpcManager.spawnAll();

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
}
