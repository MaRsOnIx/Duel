package com.me.tft_02.duel;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.me.tft_02.duel.commands.DuelCommand;
import com.me.tft_02.duel.config.Config;
import com.me.tft_02.duel.database.Data;
import com.me.tft_02.duel.hooks.FactionsListener;
import com.me.tft_02.duel.hooks.GhostsListener;
import com.me.tft_02.duel.hooks.WorldGuardListener;
import com.me.tft_02.duel.listeners.EntityListener;
import com.me.tft_02.duel.listeners.PlayerListener;
import com.me.tft_02.duel.locale.LocaleLoader;
import com.me.tft_02.duel.runnables.RegionCheckTask;
import com.me.tft_02.duel.runnables.UpdateCheckerTask;
import com.me.tft_02.duel.runnables.duels.DuelRangeTask;
import com.me.tft_02.duel.util.LogFilter;
import com.me.tft_02.duel.util.player.UserManager;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class Duel extends JavaPlugin {
    public static Duel p;

    public boolean worldGuardEnabled = false;
    public boolean ghostsEnabled = false;
    public boolean factionsEnabled = false;

    // Update Check
    public boolean updateAvailable;

    // Config Validation Check
    public boolean noErrorsInConfigFiles = true;

    /**
     * Run things on enable.
     */
    @Override
    public void onEnable() {
        p = this;
        getLogger().setFilter(new LogFilter(this));

        Config.getInstance();

        if (!noErrorsInConfigFiles) {
            return;
        }

        registerEvents();

        setupWorldGuard();
        setupFactions();
        setupGhosts();

        getCommand("duel").setExecutor(new DuelCommand());

        Data.loadData();

        for (Player player : getServer().getOnlinePlayers()) {
            UserManager.addUser(player); // In case of reload add all users back into UserManager
        }

        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, new DuelRangeTask(), 0, 2 * 20);

        if (worldGuardEnabled) {
            //Region check timer (Runs every five seconds)
            new RegionCheckTask().runTaskTimer(this, 5 * 20, 5 * 20);
        }

        checkForUpdates();
    }

    /**
     * Registers all event listeners
     */
    private void registerEvents() {
        PluginManager pluginManager = getServer().getPluginManager();

        // Register events
        pluginManager.registerEvents(new PlayerListener(), this);
        pluginManager.registerEvents(new EntityListener(), this);
    }

    public void debug(String message) {
        getLogger().info("[Debug] " + message);
    }

    private void setupWorldGuard() {
        if (getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            worldGuardEnabled = true;
            debug("WorldGuard found!");
            getServer().getPluginManager().registerEvents(new WorldGuardListener(), this);
        }
    }

    private void setupFactions() {
        if (getServer().getPluginManager().isPluginEnabled("Factions")) {
            factionsEnabled = true;
            debug("Factions found!");
            getServer().getPluginManager().registerEvents(new FactionsListener(), this);
        }
    }

    private void setupGhosts() {
        if (getServer().getPluginManager().isPluginEnabled("Ghosts")) {
            ghostsEnabled = true;
            debug("Ghosts found!");
            getServer().getPluginManager().registerEvents(new GhostsListener(), this);
        }
    }

    public WorldGuardPlugin getWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null;
        }

        return (WorldGuardPlugin) plugin;
    }

    /**
     * Run things on disable.
     */
    @Override
    public void onDisable() {
        Data.saveData();
        this.getServer().getScheduler().cancelTasks(this);
    }

    private void checkForUpdates() {
        if (!Config.getInstance().getUpdateCheckEnabled()) {
            return;
        }

        getServer().getScheduler().runTaskAsynchronously(this, new UpdateCheckerTask());
    }

    public void updateCheckerCallback(boolean updateAvailable) {
        this.updateAvailable = updateAvailable;
        if (updateAvailable) {
            getLogger().info(LocaleLoader.getString("UpdateChecker.Outdated"));
            getLogger().info(LocaleLoader.getString("UpdateChecker.New_Available"));
        }
    }
}
