package io.github.yourname.leashcompass;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CompassManager {

    private final JavaPlugin plugin;
    // holderUUID -> targetUUID
    private final Map<UUID, UUID> magnetMap = new HashMap<>();
    private BukkitTask updateTask;

    public CompassManager(JavaPlugin plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    private void startUpdateTask() {
        // обновление раз в секунду, чтобы компас "следил" за целью
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, UUID> entry : magnetMap.entrySet()) {
                Player holder = Bukkit.getPlayer(entry.getKey());
                Player target = Bukkit.getPlayer(entry.getValue());
                if (holder == null || target == null) continue;

                // проверяем, что в руке компас
                ItemStack hand = holder.getInventory().getItemInMainHand();
                if (hand.getType() == Material.COMPASS) {
                    holder.setCompassTarget(target.getLocation());
                }
            }
        }, 0L, 20L);
    }

    public boolean handleMagnetCommand(CommandSender sender, String targetName) {
        if (!(sender instanceof Player holder)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            holder.sendMessage(Component.text("Player not found."));
            return true;
        }

        if (target.equals(holder)) {
            holder.sendMessage(Component.text("Cannot magnet to yourself."));
            return true;
        }

        // проверяем наличие компаса в руке
        ItemStack hand = holder.getInventory().getItemInMainHand();
        if (hand.getType() != Material.COMPASS) {
            holder.sendMessage(Component.text("You must hold a compass in your main hand."));
            return true;
        }

        magnetMap.put(holder.getUniqueId(), target.getUniqueId());
        holder.sendMessage(Component.text("Compass magnetized to " + target.getName() + "."));
        return true;
    }

    public void removeMagnet(UUID holder) {
        magnetMap.remove(holder);
    }

    public void shutdown() {
        if (updateTask != null) updateTask.cancel();
    }
}
