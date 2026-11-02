package dev.nightfall;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.nightfall.config.Config;
import dev.nightfall.module.ModuleManager;
import dev.nightfall.modules.AutoTpa;
import dev.nightfall.modules.BaseFinder;
import dev.nightfall.module.setting.ListSetting;
import dev.nightfall.scanner.Waypoint;
import dev.nightfall.scanner.WaypointManager;
import dev.nightfall.util.Storage;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/** /nf wp | /nf tpa | /nf profile | /nf scan */
public final class Commands {
    private Commands() {}

    private static int say(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> c, String s) {
        c.getSource().sendFeedback(Text.literal("[Nightfall] " + s));
        return 1;
    }

    private static String str(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> c, String n) {
        return StringArgumentType.getString(c, n);
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> d) {
        LiteralArgumentBuilder<FabricClientCommandSource> root = literal("nf");

        // ---- Waypoints ----
        root.then(literal("wp")
                .then(literal("add")
                        .then(argument("name", StringArgumentType.word())
                                .executes(c -> addWp(c, str(c, "name"), "Default"))
                                .then(argument("category", StringArgumentType.word())
                                        .executes(c -> addWp(c, str(c, "name"), str(c, "category"))))))
                .then(literal("del")
                        .then(argument("name", StringArgumentType.word())
                                .executes(c -> say(c, WaypointManager.remove(str(c, "name")) ? "Waypoint geloescht" : "Nicht gefunden"))))
                .then(literal("list").executes(c -> {
                    String dim = Storage.dim();
                    int n = 0;
                    for (Waypoint w : WaypointManager.all()) {
                        say(c, "[" + w.category + "] " + w.name + " " + w.x + " " + w.y + " " + w.z + " (" + w.dim + ")" + (w.dim.equals(dim) ? "" : " - andere Dimension"));
                        n++;
                    }
                    return say(c, n + " Waypoints");
                })));

        // ---- AutoTPA Listen ----
        AutoTpa tpa = ModuleManager.get(AutoTpa.class);
        root.then(literal("tpa")
                .then(listCmd("wl", tpa.whitelist, "Whitelist"))
                .then(listCmd("bl", tpa.blacklist, "Blacklist"))
                .then(literal("list").executes(c -> {
                    say(c, "Whitelist: " + String.join(", ", tpa.whitelist.get()));
                    return say(c, "Blacklist: " + String.join(", ", tpa.blacklist.get()));
                })));

        // ---- Freunde (KillAura) ----
        root.then(literal("friend")
                .then(listCmd("x", ModuleManager.get(dev.nightfall.modules.KillAura.class).friends, "Freunde")));

        // ---- Profile ----
        root.then(literal("profile")
                .then(literal("save").then(argument("name", StringArgumentType.word())
                        .executes(c -> say(c, Config.saveProfile(str(c, "name")) ? "Profil gespeichert" : "Fehler beim Speichern"))))
                .then(literal("load").then(argument("name", StringArgumentType.word())
                        .executes(c -> say(c, Config.loadProfile(str(c, "name")) ? "Profil geladen" : "Profil nicht gefunden")))));

        // ---- Scanner ----
        root.then(literal("scan").then(literal("clear").executes(c -> {
            ModuleManager.get(BaseFinder.class).clearMarks();
            return say(c, "Markierungen dieses Servers/Dimension geloescht");
        })));

        d.register(root);
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> listCmd(String lit, ListSetting list, String label) {
        return literal(lit)
                .then(literal("add").then(argument("name", StringArgumentType.word())
                        .executes(c -> say(c, list.add(str(c, "name")) ? label + ": hinzugefuegt" : "Schon vorhanden"))))
                .then(literal("del").then(argument("name", StringArgumentType.word())
                        .executes(c -> say(c, list.remove(str(c, "name")) ? label + ": entfernt" : "Nicht vorhanden"))));
    }

    private static int addWp(com.mojang.brigadier.context.CommandContext<FabricClientCommandSource> c, String name, String cat) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return 0;
        WaypointManager.add(name, cat, mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
        return say(c, "Waypoint '" + name + "' gesetzt");
    }
}
