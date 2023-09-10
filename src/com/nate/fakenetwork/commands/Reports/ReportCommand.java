package com.nate.fakenetwork.commands.Reports;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.nate.fakenetwork.Core;

public class ReportCommand extends Command {
    Core core = Core.getInstance();

    public ReportCommand() {
        super("report", null, "reportplayer");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "This command can only be used by players."));
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (args.length < 2) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "Usage: /report <player> <reason>"));
            return;
        }

        ProxiedPlayer reportedPlayer = core.getProxy().getPlayer(args[0]);
        if (reportedPlayer == null) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "Player not found."));
            return;
        }

        // Remove the reported player's name from the reason
        String reason = String.join(" ", args);
        reason = reason.replaceFirst(args[0], "").trim();

        List<ProxiedPlayer> staffMembers = getStaffWithPermission("fakenetwork.reports");

        User reportingUser = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        User reportedUser = LuckPermsProvider.get().getUserManager().getUser(reportedPlayer.getUniqueId());

        // Get their prefixes
        String reportingUserPrefix = "";
        String reportedUserPrefix = "";

        if (reportingUser != null) {
            for (Node node : reportingUser.getNodes()) {
                if (node.getKey().startsWith("prefix.")) {
                    reportingUserPrefix = ChatColor.translateAlternateColorCodes('&', String.valueOf(node.getValue()));
                    break;
                }
            }
        }

        if (reportedUser != null) {
            for (Node node : reportedUser.getNodes()) {
                if (node.getKey().startsWith("prefix.")) {
                    reportedUserPrefix = ChatColor.translateAlternateColorCodes('&', String.valueOf(node.getValue()));
                    break;
                }
            }
        }

        TextComponent reportMessage = new TextComponent(ChatColor.translateAlternateColorCodes('&',
                "&4&lReport | " + reportingUserPrefix + player.getName() + " &7reported " +
                        reportedUserPrefix + reportedPlayer.getName() + ChatColor.RED + " for &4Hacking in &6server"));

        for (ProxiedPlayer staff : staffMembers) {
            staff.sendMessage(reportMessage);
        }

        storeReport(player.getName(), player.getUniqueId(), reportedPlayer.getName(), reportedPlayer.getUniqueId(),
                reason);

        TextComponent successMessage = new TextComponent(ChatColor.GREEN + "Thank you for reporting " +
                ChatColor.RESET + reportedPlayer.getName() + ChatColor.GREEN + ". Staff members have been notified.");
        sender.sendMessage(successMessage);
    }

    private List<ProxiedPlayer> getStaffWithPermission(String permission) {
        List<ProxiedPlayer> staffMembers = new ArrayList<>();
        for (ProxiedPlayer player : core.getProxy().getPlayers()) {
            if (player.hasPermission(permission)) {
                staffMembers.add(player);
            }
        }
        return staffMembers;
    }

    private void storeReport(String reporterName, UUID reporterUUID, String reportedName, UUID reportedUUID,
            String reason) {
        try {
            PreparedStatement statement = core.getConnection().prepareStatement(
                    "INSERT INTO reports (reporter_name, reporter_uuid, reported_name, reported_uuid, reason) VALUES (?, ?, ?, ?, ?)");
            statement.setString(1, reporterName);
            statement.setString(2, reporterUUID.toString());
            statement.setString(3, reportedName);
            statement.setString(4, reportedUUID.toString());
            statement.setString(5, reason);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}