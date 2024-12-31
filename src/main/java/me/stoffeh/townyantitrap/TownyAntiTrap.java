package me.stoffeh.townyantitrap;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.teleport.SuccessfulTownyTeleportEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.UUID;

public class TownyAntiTrap extends JavaPlugin implements Listener {
    private final HashMap<UUID, Integer> protectionChunks = new HashMap<>();
    private int maxChunks;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("TownyAntiTrap enabled successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("TownyAntiTrap disabled!");
    }
    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        this.maxChunks = config.getInt("protected-chunks", 8);
    }


    @EventHandler
    public void onSuccessfulTownyTeleport(SuccessfulTownyTeleportEvent event) throws NotRegisteredException {
        Resident resident = event.getResident();
        Player player = resident.getPlayer();
        UUID playerId = player.getUniqueId();

        if (player == null) {
            return;
        }
        TownyAPI townyAPI = TownyAPI.getInstance();
        Town town = null;

        try {
            TownBlock townBlock = townyAPI.getTownBlock(event.getTeleportLocation());
            if (townBlock != null) {
                if (townBlock.isHomeBlock()) {
                    town = townBlock.getTown();
                }
            }
        } catch (Exception e) {
            player.sendMessage("You are not in a town after teleporting.");
            return;
        }

        if (town != null) {
            if (resident.hasTown()) {
                if (!resident.getTown().equals(town)) {
                    if (protectionChunks.get(playerId) != null) {
                        protectionChunks.remove(playerId);
                        protectionChunks.put(playerId, maxChunks);
                    } else {
                        protectionChunks.put(playerId, maxChunks);
                    }
                    player.sendMessage(ChatColor.GOLD + "[Towny]" + ChatColor.YELLOW + " Your inventory is now protected for " + maxChunks + " chunks.");
                }
            } else { //if res is not in any town
                if (protectionChunks.get(playerId) != null) {
                    protectionChunks.remove(playerId);
                    protectionChunks.put(playerId, maxChunks);
                } else {
                    protectionChunks.put(playerId, maxChunks);
                }
                player.sendMessage(ChatColor.GOLD + "[Towny]" + ChatColor.YELLOW + " Your inventory is now protected for " + maxChunks + " chunks.");
            }
        } else {
            player.sendMessage(ChatColor.GOLD + "[Towny]" + ChatColor.RED + " An error occurred while teleporting. Your inventory is not protected!");
        }
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (protectionChunks.containsKey(playerId)) {
            Chunk fromChunk = event.getFrom().getChunk();
            Chunk toChunk = event.getTo().getChunk();

            if (!fromChunk.equals(toChunk)) {
                int remainingChunks = protectionChunks.get(playerId) - 1;
                protectionChunks.put(playerId, remainingChunks);

                if (remainingChunks == 0) {
                    protectionChunks.remove(playerId);
                    player.sendMessage(ChatColor.GOLD + "[Towny]" + ChatColor.YELLOW + " Your inventory protection has expired. You now lose your items on death!");
                    //player.sendMessage(ChatColor.GOLD + "[Towny]" + ChatColor.RED + " You have " + remainingChunks + " protected chunks remaining.");
                    //Removed 1.0.3 since it gets annoying for every town you teleport to. Maybe add preference features in the future versions
                }
            }
        }
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();
        if (protectionChunks.get(playerId) != null) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            player.sendMessage(ChatColor.GOLD + "[Towny]" + ChatColor.RED + " You died while inventory protected. You did not lose any items!");
            protectionChunks.remove(playerId);
        }
    }
}