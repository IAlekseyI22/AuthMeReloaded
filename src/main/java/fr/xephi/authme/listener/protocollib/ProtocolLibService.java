package fr.xephi.authme.listener.protocollib;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.entity.Player;

import javax.inject.Inject;

public class ProtocolLibService implements SettingsDependent, Reloadable {

    /* Packet Adapters */
    private AuthMeInventoryPacketAdapter inventoryPacketAdapter;
    private AuthMeTabCompletePacketAdapter tabCompletePacketAdapter;

    /* Settings */
    private boolean protectInvBeforeLogin;
    private boolean denyTabCompleteBeforeLogin;

    /* Service */
    private boolean isEnabled;
    private AuthMe plugin;

    @Inject
    ProtocolLibService(AuthMe plugin, NewSetting settings) {
        this.plugin = plugin;
        loadSettings(settings);
        setup();
    }

    /**
     * Set up the ProtocolLib packet adapters.
     */
    public void setup() {
        // Check if ProtocolLib is enabled on the server.
        if (!plugin.getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            if (protectInvBeforeLogin) {
                ConsoleLogger.showError("WARNING! The protectInventory feature requires ProtocolLib! Disabling it...");
            }
            
            if (denyTabCompleteBeforeLogin) {
                ConsoleLogger.showError("WARNING! The denyTabComplete feature requires ProtocolLib! Disabling it...");
            }

            this.isEnabled = false;
            return;
        }

        // Set up packet adapters
        if (protectInvBeforeLogin && inventoryPacketAdapter == null) {
            inventoryPacketAdapter = new AuthMeInventoryPacketAdapter(plugin);
            inventoryPacketAdapter.register();
        } else if (inventoryPacketAdapter != null) {
            inventoryPacketAdapter.unregister();
            inventoryPacketAdapter = null;
        }
        if (denyTabCompleteBeforeLogin && tabCompletePacketAdapter == null) {
            tabCompletePacketAdapter = new AuthMeTabCompletePacketAdapter(plugin);
            tabCompletePacketAdapter.register();
        } else if (tabCompletePacketAdapter != null) {
            tabCompletePacketAdapter.unregister();
            tabCompletePacketAdapter = null;
        }

        this.isEnabled = true;
    }

    public void disable() {
        isEnabled = false;

        if (inventoryPacketAdapter != null) {
            inventoryPacketAdapter.unregister();
            inventoryPacketAdapter = null;
        }
        if (tabCompletePacketAdapter != null) {
            tabCompletePacketAdapter.unregister();
            tabCompletePacketAdapter = null;
        }
    }

    /**
     * Send a packet to the player to give them a blank inventory.
     *
     * @param player The player to send the packet to.
     */
    public void sendBlankInventoryPacket(Player player) {
        if (isEnabled && inventoryPacketAdapter != null) {
            inventoryPacketAdapter.sendBlankInventoryPacket(player);
        }
    }

    @Override
    public void loadSettings(NewSetting settings) {
        boolean oldProtectInventory = this.protectInvBeforeLogin;

        this.protectInvBeforeLogin = settings.getProperty(RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN);
        this.denyTabCompleteBeforeLogin = settings.getProperty(RestrictionSettings.DENY_TABCOMPLETE_BEFORE_LOGIN);

        //it was true and will be deactivated now, so we need to restore the inventory for every player
        if (oldProtectInventory && !protectInvBeforeLogin) {
            inventoryPacketAdapter.unregister();
            for (Player onlinePlayer : bukkitService.getOnlinePlayers()) {
                if (!PlayerCache.getInstance().isAuthenticated(onlinePlayer.getName())) {
                    sendInventoryPacket(onlinePlayer);
                }
            }
        }
    }

    @Override
    public void reload() {
        setup();
    }
}