package io.github.yourname.leashcompass;

import org.bukkit.plugin.java.JavaPlugin;

public class LeashCompassPlugin extends JavaPlugin {

    private CompassManager compassManager;
    private LeashManager leashManager;

    @Override
    public void onEnable() {
        this.compassManager = new CompassManager(this);
        this.leashManager = new LeashManager(this);

        getCommand("compassmagnet").setExecutor((sender, cmd, label, args) -> {
            if (args.length != 1) {
                sender.sendMessage("Usage: /compassmagnet <player>");
                return true;
            }
            return compassManager.handleMagnetCommand(sender, args[0]);
        });

        getCommand("leashtarget").setExecutor((sender, cmd, label, args) -> {
            if (args.length != 1) {
                sender.sendMessage("Usage: /leashtarget <player>");
                return true;
            }
            return leashManager.handleLeashCommand(sender, args[0]);
        });

        getLogger().info("LeashCompass enabled (Paper 26.2)");
    }

    @Override
    public void onDisable() {
        if (leashManager != null) leashManager.cleanup();
    }

    public CompassManager getCompassManager() { return compassManager; }
    public LeashManager getLeashManager() { return leashManager; }
}
