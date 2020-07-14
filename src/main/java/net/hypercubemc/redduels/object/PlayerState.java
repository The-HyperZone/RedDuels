package net.hypercubemc.redduels.object;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.ArrayList;

/*
Stores the state of a player at a point in time
Can restore the player to their previous state
 */
public class PlayerState {

    private Player player;
    private ArrayList<PotionEffect> potionEffects = new ArrayList<>();
    private Location location;
    private Vector velocity;
    private float fallDistance;
    private GameMode gameMode;
    private boolean flying;
    private double health;
    private int foodLevel;
    private ItemStack[] inventoryContents;
    private ItemStack[] inventoryArmorContents;

    // records important information about a player
    public PlayerState(Player player) {
        this.player = player;
        location = player.getLocation();
        velocity = player.getVelocity();
        fallDistance = player.getFallDistance();
        gameMode = player.getGameMode();
        flying = player.isFlying();
        health = player.getHealth();
        foodLevel = player.getFoodLevel();
        inventoryContents = player.getInventory().getContents();
        inventoryArmorContents = player.getInventory().getArmorContents();
        // store potion effects
        potionEffects.addAll(player.getActivePotionEffects());
    }

    // restores everything about the player to their previous state
    public void restore() {
        player.teleport(location);
        player.setVelocity(velocity);
        player.setFallDistance(fallDistance);
        player.setGameMode(gameMode);
        player.setFlying(flying);
        player.setHealth(health);
        player.setFoodLevel(foodLevel);
        player.getInventory().setContents(inventoryContents);
        player.getInventory().setArmorContents(inventoryArmorContents);
        // restore potion effects
        for (PotionEffect e : potionEffects) {
            player.addPotionEffect(e);
        }
    }
}
