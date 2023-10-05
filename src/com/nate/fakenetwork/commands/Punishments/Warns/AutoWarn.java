package com.nate.fakenetwork.commands.Punishments.Warns;


import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.HashMap;
import java.util.Map;

import com.nate.fakenetwork.Core;

public class AutoWarn implements Listener {

    private Map<String, Long> messageTimestamps = new HashMap<>();
    private Map<String, Integer> userWarnings = new HashMap<>();

    public AutoWarn() {
        Core.getInstance().getProxy().getPluginManager().registerListener(Core.getInstance(), this);
    }

    @EventHandler
    public void onChat(ChatEvent e) {
        ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        String playerName = player.getName();

        if (isSpamming(playerName)) {
            issueWarning(playerName, "Spamming");
            notifyStaff(playerName, "Spamming");
            e.setCancelled(true);
        }
    }

    private boolean isSpamming(String playerName) {
        long currentTime = System.currentTimeMillis();
        messageTimestamps.put(playerName, currentTime);
        messageTimestamps.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > 5000);

        int messageCount = (int) messageTimestamps.values().stream()
                .filter(timestamp -> (currentTime - timestamp) <= 5000)
                .count();
        return messageCount > 3;
    }

    private void issueWarning(String playerName, String reason) {
        userWarnings.put(playerName, userWarnings.getOrDefault(playerName, 0) + 1);
        int warnings = userWarnings.getOrDefault(playerName, 0);
        if (warnings >= 5) {
            Mutes mutes = new Mutes();
            mutes.applyMute(playerName, "Spamming", 12 * 60);
            userWarnings.put(playerName, 0);
        } else {
            ProxiedPlayer player = Core.getInstance().getProxy().getPlayer(playerName);
            if (player != null) {
                String warningMessage = "[WARNING] Please don't spam or you might get muted!";
                player.sendMessage(new TextComponent(warningMessage));
            }
        }
    }

    private void notifyStaff(String playerName, String reason) {
        String message = String.format("[WARN] %s has been warned for %s. Warnings: %d", playerName, reason,
                userWarnings.getOrDefault(playerName, 0));
        for (ProxiedPlayer staff : Core.getInstance().getProxy().getPlayers()) {
            if (staff.hasPermission("fakenetwork.staff")) {
                staff.sendMessage(new TextComponent(message));
            }
        }
    }
}
