package io.github.yourname.leashcompass;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LeashManager {

    private final JavaPlugin plugin;
    private final Map<UUID, UUID> leashMap = new HashMap<>(); // targetUUID -> holderUUID
    private BukkitTask checkTask;

    private static final double MAX_DISTANCE = 10.0; // максимальное расстояние
    private static final double PULL_STRENGTH = 0.6;

    public LeashManager(JavaPlugin plugin) {
        this.plugin = plugin;
        startCheckTask();
    }

    private void startCheckTask() {
        checkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, UUID> entry : leashMap.entrySet()) {
                Player target = Bukkit.getPlayer(entry.getKey());
                Player holder = Bukkit.getPlayer(entry.getValue());
                if (target == null || holder == null) continue;
                if (!target.isOnline() || !holder.isOnline()) continue;

                Location targetLoc = target.getLocation();
                Location holderLoc = holder.getLocation();

                if (!targetLoc.getWorld().equals(holderLoc.getWorld())) continue;

                double dist = targetLoc.distance(holderLoc);
                if (dist > MAX_DISTANCE) {
                    // притягиваем цель к ведущему
                    Vector direction = holderLoc.toVector()
                            .subtract(targetLoc.toVector())
                            .normalize()
                            .multiply(PULL_STRENGTH);
                    target.setVelocity(direction);
                    target.sendActionBar(Component.text("The leash tightens... (distance: "
                            + String.format("%.1f", dist) + ")"));
                }
            }
        }, 0L, 5L); // каждые 0.25 секунды
    }

    public boolean handleLeashCommand(CommandSender sender, String targetName) {
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
            holder.sendMessage(Component.text("Cannot leash yourself."));
            return true;
        }

        leashMap.put(target.getUniqueId(), holder.getUniqueId());
        holder.sendMessage(Component.text("Leash attached to " + target.getName() + "."));
        target.sendMessage(Component.text("You have been leashed by " + holder.getName() + "."));
        return true;
    }

    public void removeLeash(UUID target) {
        leashMap.remove(target);
    }

    public void cleanup() {
        if (checkTask != null) checkTask.cancel();
        leashMap.clear();
    }
}
