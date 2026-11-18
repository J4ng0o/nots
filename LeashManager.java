package io.github.yourname.leashcompass;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LeashManager {

    private final JavaPlugin plugin;
    private final Map<UUID, LeashData> leashMap = new HashMap<>();
    private BukkitTask tickTask;

    private static final double MAX_DISTANCE = 10.0;
    private static final double PULL_STRENGTH = 0.6;

    public LeashManager(JavaPlugin plugin) {
        this.plugin = plugin;
        startTickTask();
    }

    private void startTickTask() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            leashMap.entrySet().removeIf(entry -> {
                LeashData data = entry.getValue();
                Player target = Bukkit.getPlayer(entry.getKey());
                Player holder = Bukkit.getPlayer(data.holderUUID);
                Entity proxy = data.proxy;

                if (target == null || holder == null || proxy == null || !proxy.isValid()) {
                    if (proxy != null && proxy.isValid()) proxy.remove();
                    return true;
                }

                Location targetLoc = target.getLocation().clone().add(0, 0.5, 0);
                proxy.teleport(targetLoc);

                // 距离拉扯逻辑
                Location holderLoc = holder.getLocation();
                if (!targetLoc.getWorld().equals(holderLoc.getWorld())) {
                    // 跨世界：直接解除
                    proxy.remove();
                    return true;
                }

                double dist = targetLoc.distance(holderLoc);
                if (dist > MAX_DISTANCE) {
                    Vector direction = holderLoc.toVector()
                            .subtract(targetLoc.toVector())
                            .normalize()
                            .multiply(PULL_STRENGTH);
                    target.setVelocity(direction);
                    target.sendActionBar(Component.text("The leash tightens... (" +
                            String.format("%.1f", dist) + "m)"));
                }
                return false;
            });
        }, 0L, 1L);
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

        removeLeash(target.getUniqueId());

        Slime proxy = target.getWorld().spawn(target.getLocation(), Slime.class);
        proxy.setSize(1);
        proxy.setInvisible(true);
        proxy.setSilent(true);
        proxy.setInvulnerable(true);
        proxy.setGravity(false);
        proxy.setCollidable(false);
        proxy.setAI(false);

        proxy.setLeashHolder(holder);

        leashMap.put(target.getUniqueId(), new LeashData(holder.getUniqueId(), proxy));
        holder.sendMessage(Component.text("Leash attached to " + target.getName() + "."));
        target.sendMessage(Component.text("You have been leashed by " + holder.getName() + "."));
        return true;
    }

    public void removeLeash(UUID targetUUID) {
        LeashData data = leashMap.remove(targetUUID);
        if (data != null && data.proxy != null && data.proxy.isValid()) {
            data.proxy.remove();
        }
    }

    public void cleanup() {
        if (tickTask != null) tickTask.cancel();
        for (LeashData data : leashMap.values()) {
            if (data.proxy != null && data.proxy.isValid()) data.proxy.remove();
        }
        leashMap.clear();
    }

    private static class LeashData {
        final UUID holderUUID;
        final Entity proxy;
        LeashData(UUID holderUUID, Entity proxy) {
            this.holderUUID = holderUUID;
            this.proxy = proxy;
        }
    }
}
