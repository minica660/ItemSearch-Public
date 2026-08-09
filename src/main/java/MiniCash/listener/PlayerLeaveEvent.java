package MiniCash.listener;

import MiniCash.Database.DatabaseManager;
import MiniCash.ItemSearch;
import MiniCash.model.ContainerModel;
import MiniCash.model.ItemData;
import MiniCash.util.CustomModelDataUtil;
import MiniCash.util.ItemSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

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

        String containerId = "PLAYER_INVENTORY_" + plugin.getServer().getName() + "_" + player.getUniqueId();
        Map<String, ItemData> itemMap = new HashMap<>();

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            String base64 = ItemSerializer.itemSerializer(item);
            String hash = ItemSerializer.getMD5Hash(base64);

            if (itemMap.containsKey(hash)) {
                itemMap.get(hash).addAmount(item.getAmount());
            } else {
                ItemMeta itemMeta = item.getItemMeta();
                Integer customModelData = CustomModelDataUtil.getCustomModelData(itemMeta);
                String displayName = null;

                if (itemMeta != null && itemMeta.hasDisplayName()) {
                    displayName = PlainTextComponentSerializer.plainText().serialize(itemMeta.itemName());
                }

                ItemData itemData = new ItemData(
                        hash,
                        item.getType().name(),
                        item.getAmount(),
                        base64,
                        customModelData,
                        displayName,
                        0
                );
                itemMap.put(hash, itemData);
            }

            // シュルカーボックス
            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta instanceof BlockStateMeta blockStateMeta && blockStateMeta.getBlockState() instanceof org.bukkit.block.ShulkerBox shulkerBox) {
                for (ItemStack subItem : shulkerBox.getInventory().getContents()) {
                    if (subItem == null || subItem.getType().isAir()) {
                        continue;
                    }

                    String subBase64 = ItemSerializer.itemSerializer(subItem);
                    String subHash = ItemSerializer.getMD5Hash(subBase64);

                    if (itemMap.containsKey(subHash)) {
                        itemMap.get(subHash).addAmount(subItem.getAmount());
                    } else {
                        ItemMeta subMeta = subItem.getItemMeta();
                        Integer subCustomModelData = CustomModelDataUtil.getCustomModelData(subMeta);
                        String subDisplayName = null;

                        if (subMeta != null && subMeta.hasDisplayName()) {
                            subDisplayName = PlainTextComponentSerializer.plainText().serialize(subMeta.itemName());
                        }

                        ItemData subItemData = new ItemData(
                                subHash,
                                subItem.getType().name(),
                                subItem.getAmount(),
                                null,
                                subCustomModelData,
                                subDisplayName,
                                1
                        );
                        itemMap.put(subHash, subItemData);
                    }
                }
            }
        }

        ContainerModel containerModel = new ContainerModel(
                containerId, "PLAYER",
                player.getName(), player.getUniqueId().toString(),
                plugin.getServer().getName(), player.getWorld().getName(),
                null, null, null, itemMap
        );

        // DBに保存
        DatabaseManager.saveContainerData(containerModel);

    }

}
