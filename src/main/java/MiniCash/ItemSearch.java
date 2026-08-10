package MiniCash;

import MiniCash.Database.DatabaseManager;
import MiniCash.listener.BlockUpdateEvent;
import MiniCash.listener.InventoryClosedEvent;
import MiniCash.listener.PlayerLeaveEvent;
import MiniCash.util.ItemSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemSearch extends JavaPlugin {

    private static String serverName;

    @Override
    public void onEnable() {
        // Plugin startup logic

        new ItemSerializer(this);
        new DatabaseManager(this);

        saveDefaultConfig();

        ItemSearch.serverName = getConfig().getString("server-name");

        DatabaseManager.connect();

        getServer().getPluginManager().registerEvents(new InventoryClosedEvent(this), this);
        getServer().getPluginManager().registerEvents(new PlayerLeaveEvent(this), this);
        getServer().getPluginManager().registerEvents(new BlockUpdateEvent(this), this);

        registerCommand("itemsearch",new MiniCash.commands.ItemSearch(this));


        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            DatabaseManager.logWholeServerItemCount(5);
        }, 1200L, 72000L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        DatabaseManager.disConnect();
    }

    public static Component getMessage(Component message){
        return Component.text("[").color(NamedTextColor.GRAY).append(Component.text("ItemSearch").color(NamedTextColor.GREEN).append(Component.text("]").color(NamedTextColor.GRAY)
                .append(message)
        ));
    }

    public static String getServerName(){
        return serverName;
    }

}
