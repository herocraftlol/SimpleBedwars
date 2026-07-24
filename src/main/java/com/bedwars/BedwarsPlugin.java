package com.bedwars;

import com.bedwars.arena.ArenaManager;
import com.bedwars.commands.BedwarsCommand;
import com.bedwars.game.GameManager;
import com.bedwars.gui.AdminGUIManager;
import com.bedwars.gui.AdminNPCManager;
import com.bedwars.listeners.CombatListener;
import com.bedwars.listeners.GUIListener;
import com.bedwars.listeners.OreMergeListener;
import com.bedwars.listeners.PlayerProtectionListener;
import com.bedwars.scoreboard.ScoreboardManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BedwarsPlugin extends JavaPlugin {

    private static BedwarsPlugin instance;

    private ArenaManager arenaManager;
    private GameManager gameManager;
    private ScoreboardManager scoreboardManager;
    private AdminNPCManager adminNPCManager;
    private AdminGUIManager adminGUIManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.arenaManager = new ArenaManager(this);
        this.scoreboardManager = new ScoreboardManager();
        this.gameManager = new GameManager(this);
        this.adminNPCManager = new AdminNPCManager(this);
        this.adminGUIManager = new AdminGUIManager(this);

        arenaManager.loadAll();
        adminNPCManager.load();

        BedwarsCommand command = new BedwarsCommand(this);
        getCommand("bd").setExecutor(command);
        getCommand("bd").setTabCompleter(command);

        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new OreMergeListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerProtectionListener(this), this);

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
}
