package com.yangline.afkplusvelocity;

import net.lapismc.afkplus.api.AFKPlusAPI;
import net.lapismc.afkplus.api.AFKStartEvent;
import net.lapismc.afkplus.api.AFKStopEvent;
import net.lapismc.afkplus.playerdata.AFKPlusPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.logging.Level;

public final class AFKPlusBukkit extends JavaPlugin implements PluginMessageListener, Listener {

    private static final String CHANNEL = "afkplusvelocity:afk-broadcast";

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
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        // Reg event handler
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("AFKPlusVelocity has been enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic, unreg channel
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this, CHANNEL, this);
        getLogger().info("AFKPlusVelocity has been disabled!");
    }

    @Override
    public void onPluginMessageReceived(@NotNull String s, @NotNull Player player, byte @NotNull [] bytes) {
        if (!s.equals(CHANNEL)) return;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            EventStat stat = in.readUTF().equals(EventStat.ENTER.get()) ? EventStat.ENTER : EventStat.EXIT;
            String playerName = in.readUTF();
            String message = in.readUTF();
            // Broadcast AFK message
            Bukkit.broadcastMessage(new AFKPlusAPI().getPlugin().config
                    .getMessage(stat.equals(EventStat.ENTER) ? "Broadcast.Start" : "Broadcast.Stop").replaceAll("\\{PLAYER}", playerName));
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
//        getLogger().warning("Player: " + player + ", " + message + ", " + stat.toString());
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
