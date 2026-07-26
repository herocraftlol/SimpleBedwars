package com.bedwars;

import com.bedwars.arena.ArenaManager;
import com.bedwars.commands.BedwarsCommand;
import com.bedwars.game.GameManager;
import com.bedwars.gui.JoinGUIManager;
import com.bedwars.listeners.CombatListener;
import com.bedwars.listeners.GUIListener;
import com.bedwars.listeners.OreMergeListener;
import com.bedwars.listeners.PlayerProtectionListener;
import com.bedwars.listeners.ShopListener;
import com.bedwars.npc.HumanNPCManager;
import com.bedwars.scoreboard.ScoreboardManager;
import com.bedwars.shop.ShopConfigManager;
import com.bedwars.shop.ShopGUIManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BedwarsPlugin extends JavaPlugin {

    private static BedwarsPlugin instance;

    private ArenaManager arenaManager;
    private GameManager gameManager;
    private ScoreboardManager scoreboardManager;
    private JoinGUIManager joinGUIManager;
    private ShopConfigManager shopConfigManager;
    private ShopGUIManager shopGUIManager;
    private HumanNPCManager humanNPCManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.arenaManager = new ArenaManager(this);
        this.scoreboardManager = new ScoreboardManager();
        this.gameManager = new GameManager(this);
        this.joinGUIManager = new JoinGUIManager(this);
        this.shopConfigManager = new ShopConfigManager(this);
        this.shopGUIManager = new ShopGUIManager(this);
        this.humanNPCManager = new HumanNPCManager(this);

        arenaManager.loadAll();
        shopConfigManager.load();
        humanNPCManager.load();

        BedwarsCommand command = new BedwarsCommand(this);
        getCommand("bd").setExecutor(command);
        getCommand("bd").setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new OreMergeListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);

        getLogger().info("BedwarsPlugin activé — " + arenaManager.getArenas().size() + " arène(s) chargée(s).");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdownAll();
        }
        if (shopConfigManager != null) {
            shopConfigManager.save();
        }
        if (humanNPCManager != null) {
            humanNPCManager.removeAll();
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

    public JoinGUIManager getJoinGUIManager() {
        return joinGUIManager;
    }

    public ShopConfigManager getShopConfigManager() {
        return shopConfigManager;
    }

    public ShopGUIManager getShopGUIManager() {
        return shopGUIManager;
    }

    public HumanNPCManager getHumanNPCManager() {
        return humanNPCManager;
    }
}
