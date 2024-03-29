package com.notryken.claimpoints.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.notryken.claimpoints.ClaimPoints;
import com.notryken.claimpoints.util.MsgScanner;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.regex.Pattern;

import static com.notryken.claimpoints.ClaimPoints.config;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class Commands {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("cp")
                .then(literal("help")
                        .executes(ctx -> showHelp()))
                .then(literal("waypoints")
                        .then(literal("show")
                                .executes(ctx -> showClaimPoints()))
                        .then(literal("hide")
                                .executes(ctx -> hideClaimPoints()))
                        .then(literal("clear")
                                .executes(ctx -> clearClaimPoints()))
                        .then(literal("set")
                                .then(literal("nameformat")
                                        .then(argument("name format", StringArgumentType.greedyString())
                                                .executes(ctx -> setNameFormat(StringArgumentType.getString(ctx, "name format")))))
                                .then(literal("alias")
                                        .then(argument("alias", StringArgumentType.greedyString())
                                                .executes(ctx -> setAlias(StringArgumentType.getString(ctx, "alias")))))
                                .then(literal("color")
                                        .then(argument("color", StringArgumentType.greedyString())
                                                .suggests(((context, builder) -> SharedSuggestionProvider.suggest(ClaimPoints.waypointColorNames, builder)))
                                                .executes(ctx -> setColor(StringArgumentType.getString(ctx, "color")))))))
                .then(literal("worlds")
                        .executes(ctx -> getWorlds()))
                .then(literal("add")
                        .then(argument("world name", StringArgumentType.greedyString())
                                .suggests(((context, builder) -> SharedSuggestionProvider.suggest(MsgScanner.getWorlds(), builder)))
                                .executes(ctx -> addFrom(StringArgumentType.getString(ctx, "world name")))))
                .then(literal("clean")
                        .then(argument("world name", StringArgumentType.greedyString())
                                .suggests(((context, builder) -> SharedSuggestionProvider.suggest(MsgScanner.getWorlds(), builder)))
                                .executes(ctx -> cleanFrom(StringArgumentType.getString(ctx, "world name")))))
                .then(literal("update")
                        .then(argument("world name", StringArgumentType.greedyString())
                                .suggests(((context, builder) -> SharedSuggestionProvider.suggest(MsgScanner.getWorlds(), builder)))
                                .executes(ctx -> updateFrom(StringArgumentType.getString(ctx, "world name"))))));
    }

    private static int showHelp() {
        MutableComponent msg = Component.empty().withStyle(ChatFormatting.DARK_GRAY);
        msg.append("\n============= ");
        msg.append(Component.literal("Claim").withStyle(ChatFormatting.AQUA));
        msg.append(Component.literal("Points").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(" by ");
        msg.append(Component.literal("NotRyken").withStyle(ChatFormatting.GRAY));
        msg.append(" =============\n");
        msg.append(Component.literal("/cp worlds\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Lists the GriefPrevention worlds in which you have active claims, and " +
                        "stores them for future autocompletion.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp add <world>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Adds the northwest-corner coordinate of all claims in the specified " +
                        "GriefPrevention world as Xaero's Minimap waypoints in the active waypoint list.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp clean <world>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Removes all ClaimPoints in the active waypoint list, " +
                        "that do not match a claim in the specified world.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp update <world>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Combines /cp add <world> and /cp clean <world>, and also updates " +
                        "ClaimPoint size indicators.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints show\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Enables (shows) all ClaimPoints in the active waypoint list.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints hide\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Disables (hides) all ClaimPoints in the active waypoint list.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints clear\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Permanently deletes all ClaimPoints in the active waypoint list.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints set nameformat <name format>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Sets the name format of all ClaimPoints to the specified value. " +
                        "Note: the name format must contain %d.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints set alias <alias>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Sets the alias (symbol) of all ClaimPoints to the specified value.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("-----------------------------------------------\n");
        msg.append(Component.literal("/cp waypoints set color <color>\n").withStyle(ChatFormatting.DARK_AQUA));
        msg.append(Component.literal("Sets the color of all ClaimPoints to the specified value.\n")
                .withStyle(ChatFormatting.GRAY));
        msg.append("===============================================\n");
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(msg);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int showClaimPoints() {
        ClaimPoints.waypointManager.showClaimPoints();
        return Command.SINGLE_SUCCESS;
    }

    private static int hideClaimPoints() {
        ClaimPoints.waypointManager.hideClaimPoints();
        return Command.SINGLE_SUCCESS;
    }

    private static int clearClaimPoints() {
        int removed = ClaimPoints.waypointManager.clearClaimPoints();
        MutableComponent msg = ClaimPoints.PREFIX.copy();
        msg.append("Removed all ClaimPoints (" + removed + ").");
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(msg);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int setNameFormat(String nameFormat) {
        MutableComponent msg = ClaimPoints.PREFIX.copy();
        int indexOfSize = nameFormat.indexOf("%d");
        if (indexOfSize != -1) {
            ClaimPoints.waypointManager.setClaimPointNameFormat(nameFormat);
            config().cpSettings.nameFormat = nameFormat;
            config().cpSettings.namePattern = "^" + Pattern.quote(nameFormat.substring(0, indexOfSize)) +
                    "(\\d+)" + Pattern.quote(nameFormat.substring(indexOfSize + 2)) + "$";
            config().cpSettings.nameCompiled = Pattern.compile(config().cpSettings.namePattern);
            config().writeToFile();
            msg.append("Set ClaimPoint name format to '" + nameFormat + "'.");
        }
        else {
            msg.append("'" + nameFormat + "' is not a valid name format. Requires %d for claim size.");
        }

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(msg);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int setAlias(String alias) {
        alias = alias.length() <= 2 ? alias : alias.substring(0, 2);
        ClaimPoints.waypointManager.setClaimPointAlias(alias);
        config().cpSettings.alias = alias;
        config().writeToFile();
        MutableComponent msg = ClaimPoints.PREFIX.copy();
        msg.append("Set alias of all ClaimPoints to " + alias);
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(msg);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int setColor(String color) {
        MutableComponent msg = ClaimPoints.PREFIX.copy();
        int index = ClaimPoints.waypointColorNames.indexOf(color);
        if (index == -1) {
            msg.append("'" + color + "' is not a valid color ID.");
        }
        else {
            ClaimPoints.waypointManager.setClaimPointColor(index);
            config().cpSettings.color = color;
            config().cpSettings.colorIdx = index;
            config().writeToFile();
            msg.append("Set color of all ClaimPoints to " + color);
        }

        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.sendSystemMessage(msg);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int getWorlds() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.sendCommand("claimlist");
            MsgScanner.startWorldScan();
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int addFrom(String world) {
        return scanFrom(world, MsgScanner.ScanType.ADD);
    }

    private static int cleanFrom(String world) {
        return scanFrom(world, MsgScanner.ScanType.CLEAN);
    }

    private static int updateFrom(String world) {
        return scanFrom(world, MsgScanner.ScanType.UPDATE);
    }

    private static int scanFrom(String world, MsgScanner.ScanType scanType) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.sendCommand("claimlist");
            MsgScanner.startClaimScan(world, scanType);
        }
        return Command.SINGLE_SUCCESS;
    }
}
