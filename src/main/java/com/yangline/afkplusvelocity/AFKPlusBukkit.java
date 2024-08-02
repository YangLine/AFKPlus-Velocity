package com.yangline.afkplusvelocity;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.lapismc.afkplus.api.AFKPlusAPI;
import net.lapismc.afkplus.api.AFKStartEvent;
import net.lapismc.afkplus.api.AFKStopEvent;
import net.lapismc.afkplus.playerdata.AFKPlusPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.logging.Level;

public final class AFKPlusBukkit extends JavaPlugin implements PluginMessageListener, Listener {

    private static final String CHANNEL = "afkplusvelocity:afk";

    public enum EventStat {
        ENTER("y"),
        EXIT("n");

        private final String string;

        EventStat(String string) {
            this.string = string;
        }

        public String get() {
            return string;
        }
    }

    @Override
    public void onEnable() {
        // Plugin startup logic, reg channel
        saveDefaultConfig();
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        // PlaceholderAPI
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) new Placeholders().register();
        else getLogger().warning("Placeholder not installed, PAPI feature will be disabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic, unreg channel
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this, CHANNEL, this);
    }

    private class Placeholders extends PlaceholderExpansion {
        @Override
        public @NotNull String getAuthor() {
            return String.join(", ", AFKPlusBukkit.this.getDescription().getAuthors());
        }

        @Override
        public @NotNull String getIdentifier() {
            return "afkplusvelocity";
        }

        @Override
        public @NotNull String getVersion() {
            return AFKPlusBukkit.this.getDescription().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
            if (params.equalsIgnoreCase("afk"))
                return AFKPlusBukkit.this.getConfig().getString(new AFKPlusAPI().getPlugin().getPlayer(player.getUniqueId()).isAFK() ? "true" : "false");
            return null;
        }
    }

    @Override
    public void onPluginMessageReceived(@NotNull String s, @NotNull Player player, byte @NotNull [] bytes) {
        if (!s.equals(CHANNEL)) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            EventStat stat = EventStat.valueOf(in.readUTF());
            String playerName = in.readUTF();
            String message = in.readUTF();
            Bukkit.broadcastMessage(message); // Broadcast AFK message
        } catch (IOException | IllegalArgumentException e) {
            getLogger().log(Level.WARNING, "An internal error occurred\n", e);
        }
    }

    @EventHandler
    public void onAFKStart(@NotNull AFKStartEvent event) {
        onAFK(event.getPlayer(), event.getBroadcastMessage(), EventStat.ENTER);
    }

    @EventHandler
    public void onAFKStopEvent(@NotNull AFKStopEvent event) {
        onAFK(event.getPlayer(), event.getBroadcastMessage(), EventStat.EXIT);
    }

    public void onAFK(AFKPlusPlayer AFKPlayer, String message, EventStat stat) {
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(byteArray);
        Player player = Bukkit.getPlayer(AFKPlayer.getUUID());
        if (player == null) return;
        try {
            out.writeUTF(stat.get()); // Stop
            out.writeUTF(player.getName()); // Player name
            out.writeUTF(message);
            player.sendPluginMessage(this, CHANNEL, byteArray.toByteArray());
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "An internal error occurred\n", e);
        }
    }
}
