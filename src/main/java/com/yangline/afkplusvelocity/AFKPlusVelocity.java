package com.yangline.afkplusvelocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.logging.Logger;

@Plugin(id = "afkplusvelocity", name = "AFKPlusVelocity", version = "1.0", authors = "Yang Line")
public class AFKPlusVelocity {
    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("afkplusvelocity", "afk-broadcast");

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public AFKPlusVelocity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("AFKPlusVelocity has been enabled!");
        server.getChannelRegistrar().register(CHANNEL);
        server.getEventManager().register(this, new MessageListener(server, logger));
    }

    @Subscribe
    public void onProxyShutdownEvent(ProxyShutdownEvent event) {
        logger.info("AFKPlusVelocity has been disabled!");
    }

    public class MessageListener {
        private final ProxyServer server;
        private final Logger logger;

        public MessageListener(ProxyServer server, Logger logger) {
            this.server = server;
            this.logger = logger;
        }

        @Subscribe
        public void onPluginMessage(@NotNull PluginMessageEvent event) {
            if (event.getIdentifier().equals(CHANNEL)) {
                Player sender = event.getSource() instanceof Player ? (Player) event.getSource() : null;

                for (RegisteredServer server : this.server.getAllServers()) {
                    if (server != null) {
                        // Exclude servers from sending messages
                        if (sender != null && sender.getCurrentServer().map(s ->
                                s.getServerInfo().equals(server.getServerInfo())).orElse(false)) continue;
                        server.sendPluginMessage(CHANNEL, event.getData());
                    }
                }
                if (sender != null) {
                    logger.info("Sent AFK message of \"" + sender.getUsername() + "\" success");
                }
            }
        }
    }
}
