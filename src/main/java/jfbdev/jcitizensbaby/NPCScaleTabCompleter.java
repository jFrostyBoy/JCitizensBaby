package jfbdev.jcitizensbaby;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class NPCScaleTabCompleter implements TabCompleter {

    private final TabCompleter original;
    private final JCitizensBaby plugin;

    public NPCScaleTabCompleter(TabCompleter original, JCitizensBaby plugin) {
        this.original = original;
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, Command command, @NonNull String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("npc")) {
            return original != null ? original.onTabComplete(sender, command, alias, args) : Collections.emptyList();
        }

        if (!sender.hasPermission("jcitizensbaby.use")) {
            return Collections.emptyList();
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("scale")) {
            if (args.length == 2) {
                String partial = args[1].toLowerCase(Locale.ROOT);
                List<String> suggestions = new ArrayList<>();
                if ("baby".startsWith(partial)) suggestions.add("baby");
                if ("normal".startsWith(partial)) suggestions.add("normal");
                return suggestions;
            }

            if (args.length == 3 && args[1].equalsIgnoreCase("baby")) {
                String partial = args[2].toLowerCase(Locale.ROOT);
                List<String> suggestions = new ArrayList<>();
                for (double d : plugin.getConfig().getDoubleList("scales.suggestions")) {
                    String s = String.valueOf(d);
                    if (s.startsWith(partial)) {
                        suggestions.add(s);
                    }
                }
                Collections.sort(suggestions);
                return suggestions;
            }
        }

        return original != null ? original.onTabComplete(sender, command, alias, args) : Collections.emptyList();
    }
}