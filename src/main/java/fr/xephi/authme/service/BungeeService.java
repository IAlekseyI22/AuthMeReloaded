package fr.xephi.authme.service;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.hooks.BungeeCordMessage;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.util.BukkitService;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;

import javax.inject.Inject;

/**
 * Class to manage all BungeeCord related processes.
 */
public class BungeeService implements SettingsDependent {

    private AuthMe plugin;
    private BukkitService bukkitService;
    private BungeeCordMessage bungeeCordMessage;

    private boolean isEnabled;
    private String bungeeServer;

    /*
     * Constructor.
     */
    @Inject
    BungeeService(AuthMe plugin, BukkitService bukkitService, Settings settings, BungeeCordMessage bungeeCordMessage) {
        this.plugin = plugin;
        this.bukkitService = bukkitService;
        this.bungeeCordMessage = bungeeCordMessage;
        reload(settings);
    }

    /**
     * Sends a Bungee message to a player, e.g. login.
     *
     * @param player The player to send the message to.
     * @param action The action to send, e.g. login.
     */
    public void sendBungeeMessage(Player player, String action) {
        if (!isEnabled) {
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("AuthMe");
        out.writeUTF(action + ";" + player.getName());
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    /**
     * Send a Bungee message for a password change.
     *
     * @param player The player who's password is changed.
     * @param password The new password.
     */
    public void sendPasswordChanged(final Player player, HashedPassword password) {
        if (!isEnabled) {
            return;
        }

        final String hash = password.getHash();
        final String salt = password.getSalt();

        bukkitService.scheduleSyncDelayedTask(new Runnable() {
            @Override
            public void run() {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Forward");
                out.writeUTF("ALL");
                out.writeUTF("AuthMe");
                out.writeUTF("changepassword;" + player.getName() + ";" + hash + ";" + salt);
                player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            }
        });
    }

    /**
     * Send a player to a specified server. If no server is configured, this will
     * do nothing.
     *
     * @param player The player to send.
     */
    public void connectPlayer(Player player) {
        if (!isEnabled || bungeeServer.isEmpty()) {
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(bungeeServer);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    @Override
    public void reload(Settings settings) {
        this.isEnabled = settings.getProperty(HooksSettings.BUNGEECORD);
        this.bungeeServer = settings.getProperty(HooksSettings.BUNGEECORD_SERVER);
        Messenger messenger = plugin.getServer().getMessenger();
        if (!this.isEnabled) {
            return;
        }
        if (!messenger.isIncomingChannelRegistered(plugin, "BungeeCord")) {
            messenger.registerIncomingPluginChannel(plugin, "BungeeCord", bungeeCordMessage);
        }
        if (!messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")) {
            messenger.registerOutgoingPluginChannel(plugin, "BungeeCord");
        }
    }
}
