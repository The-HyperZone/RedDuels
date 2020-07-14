package net.hypercubemc.redduels;

import net.hypercubemc.redduels.command.CommandAcceptDuel;
import net.hypercubemc.redduels.command.CommandDuel;
import net.hypercubemc.redduels.command.CommandDuelTypes;
import net.hypercubemc.redduels.command.CommandMuteDuel;
import net.hypercubemc.redduels.listener.CleanUpListener;
import net.hypercubemc.redduels.object.SharedData;
import net.hypercubemc.redduels.object.Duel;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Logger;

public final class RedDuels extends JavaPlugin {

    SharedData sharedData;

    @Override
    public void onEnable() {
        // make a config file if it doesn't exist
        this.saveDefaultConfig();
        // initialize our shared data object
        sharedData = new SharedData(this);
        // schedule a repeating task that removes expired duels from queuedDuels
        new BukkitRunnable() {
            @Override
            public void run() {
                Duel.removeExpiredDuels(sharedData);
            }
        }.runTaskTimer(this, 40, 40);
        // register commands
        this.getCommand("duel").setExecutor(new CommandDuel(sharedData));
        this.getCommand("acceptduel").setExecutor(new CommandAcceptDuel(sharedData));
        this.getCommand("muteduel").setExecutor(new CommandMuteDuel(sharedData));
        this.getCommand("dueltypes").setExecutor(new CommandDuelTypes(sharedData));
        // register listeners
        getServer().getPluginManager().registerEvents(new CleanUpListener(sharedData), this);

        Logger logger = Bukkit.getLogger();
        logger.info("[RedDuels] Plugin enabled.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Logger logger = Bukkit.getLogger();
        logger.info("[RedDuels] Plugin disabled.");
    }
}
