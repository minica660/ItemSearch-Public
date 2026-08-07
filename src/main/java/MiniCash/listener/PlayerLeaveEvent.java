package MiniCash.listener;

import MiniCash.ItemSearch;
import MiniCash.model.ContainerModel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class PlayerLeaveEvent implements Listener {

    private final ItemSearch plugin;
    public PlayerLeaveEvent(ItemSearch plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        Inventory inventory = player.getInventory();

        String containerId = "PLAYER_" + plugin.getServer().getName() + "_" + player.getUniqueId();
        Map<Integer, ItemStack> items = new HashMap<>();

        for (int slot = 0; slot < inventory.getSize(); slot++) {

            ItemStack item = inventory.getItem(slot);

            if (item != null && !item.getType().isAir()) {

                items.put(slot, item.clone());

            }
        }

        ContainerModel containerModel = new ContainerModel(
                containerId, "PLAYER",
                player.getName(), player.getUniqueId().toString(),
                plugin.getServer().getName(), player.getWorld().getName(), null, null, null, items
        );


        // ここでDBの追加メソッドを呼び出す


    }

}
