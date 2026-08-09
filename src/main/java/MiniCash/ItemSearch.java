package MiniCash;

import MiniCash.Database.DatabaseManager;
import MiniCash.listener.BlockUpdateEvent;
import MiniCash.listener.InventoryClosedEvent;
import MiniCash.listener.PlayerLeaveEvent;
import MiniCash.util.ItemSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemSearch extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic

        new ItemSerializer(this);
        new DatabaseManager(this);

        saveDefaultConfig();

        DatabaseManager.connect();

        getServer().getPluginManager().registerEvents(new InventoryClosedEvent(this), this);
        getServer().getPluginManager().registerEvents(new PlayerLeaveEvent(this), this);
        getServer().getPluginManager().registerEvents(new BlockUpdateEvent(this), this);

        registerCommand("itemsearch",new MiniCash.commands.ItemSearch(this));


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

}
