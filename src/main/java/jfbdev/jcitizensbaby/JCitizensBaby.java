package jfbdev.jcitizensbaby;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.event.NPCSpawnEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class JCitizensBaby extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(ScaleTrait.class).withName("scale"));

        getServer().getPluginManager().registerEvents(this, this);

        PluginCommand reloadCmd = getCommand("jcbreload");
        if (reloadCmd != null) {
            reloadCmd.setExecutor(this);
            reloadCmd.setTabCompleter(this);
        }

        try {
            Method getCommandMapMethod = getServer().getClass().getMethod("getCommandMap");
            SimpleCommandMap commandMap = (SimpleCommandMap) getCommandMapMethod.invoke(getServer());

            Command npcCommand = commandMap.getCommand("npc");
            if (npcCommand instanceof PluginCommand pluginNpcCmd) {
                TabCompleter originalNPCCompleter = pluginNpcCmd.getTabCompleter();
                pluginNpcCmd.setTabCompleter(new NPCScaleTabCompleter(originalNPCCompleter, this));
                getLogger().info("Таб-комплит для /npc scale успешно подключён.");
            } else {
                getLogger().warning("Таб-комплит для /npc scale не подключён.");
            }
        } catch (Exception e) {
            getLogger().log(java.util.logging.Level.WARNING, "Не удалось подключить таб-комплит к /npc", e);
        }

        getServer().getScheduler().runTaskLater(this, () -> {
            for (NPC npc : CitizensAPI.getNPCRegistry()) {
                if (npc.hasTrait(ScaleTrait.class)) {
                    ScaleTrait trait = npc.getOrAddTrait(ScaleTrait.class);
                    trait.applyScale();
                }
            }
            getLogger().info("Масштабы NPC применены после загрузки плагина.");
        }, 40L);
    }

    private String getMessage(String path, String... replacements) {
        String msg = getConfig().getString("messages." + path, "&cСообщение не найдено: " + path);
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        if (replacements != null) {
            for (int i = 0; i < replacements.length; i += 2) {
                msg = msg.replace(replacements[i], replacements[i + 1]);
            }
        }
        return msg;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String[] parts = message.split("\\s+");
        if (parts.length < 3 || !parts[1].equalsIgnoreCase("scale")) return;

        Player player = event.getPlayer();

        if (!player.hasPermission("jcitizensbaby.use")) {
            player.sendMessage(getMessage("no-permission"));
            event.setCancelled(true);
            return;
        }

        NPC npc = CitizensAPI.getDefaultNPCSelector().getSelected(player);
        if (npc == null) {
            player.sendMessage(getMessage("select-npc"));
            event.setCancelled(true);
            return;
        }

        if (npc.isSpawned() && !(npc.getEntity() instanceof LivingEntity)) {
            player.sendMessage(getMessage("not-living-entity"));
            event.setCancelled(true);
            return;
        }

        if (!npc.isSpawned()) {
            player.sendMessage(getMessage("not-spawned"));
        }

        ScaleTrait trait = npc.getOrAddTrait(ScaleTrait.class);

        double newScale = 1.0;
        boolean valid = true;

        double normalScale = getConfig().getDouble("scales.normal", 1.0);

        if (parts.length == 3) {
            String arg = parts[2].toLowerCase(Locale.ROOT);
            if (arg.equals("normal")) {
                newScale = normalScale;
            } else if (arg.equals("baby")) {
                newScale = getConfig().getDouble("scales.baby-default", 0.5);
            } else {
                valid = false;
            }
        } else if (parts.length == 4 && parts[2].equalsIgnoreCase("baby")) {
            try {
                newScale = Double.parseDouble(parts[3]);
                if (newScale <= 0) {
                    player.sendMessage(getMessage("invalid-scale-zero"));
                    event.setCancelled(true);
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(getMessage("invalid-scale", "{input}", parts[3]));
                event.setCancelled(true);
                return;
            }
        } else {
            valid = false;
        }

        if (!valid) {
            player.sendMessage(getMessage("invalid-usage"));
            event.setCancelled(true);
            return;
        }

        trait.setScale(newScale);

        String type = (newScale == normalScale) ? "normal" : "baby";
        String scaleStr = String.format("%.2f", newScale);
        player.sendMessage(getMessage("success-scale", "{type}", type, "{scale}", scaleStr));

        event.setCancelled(true);
    }

    @EventHandler
    public void onNPCSpawn(NPCSpawnEvent event) {
        NPC npc = event.getNPC();
        if (npc.hasTrait(ScaleTrait.class)) {
            npc.getOrAddTrait(ScaleTrait.class).applyScale();
        }
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, Command command, @NonNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("jcbreload")) {
            if (!sender.hasPermission("jcitizensbaby.use")) {
                sender.sendMessage(getMessage("no-permission"));
                return true;
            }

            reloadConfig();
            sender.sendMessage(getMessage("reload-success"));
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias, String[] args) {
        return Collections.emptyList();
    }
}