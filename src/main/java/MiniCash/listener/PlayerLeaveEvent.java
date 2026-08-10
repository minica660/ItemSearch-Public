package MiniCash.listener;

import MiniCash.Database.DatabaseManager;
import MiniCash.ItemSearch;
import MiniCash.model.ContainerModel;
import MiniCash.model.ItemData;
import MiniCash.util.CustomModelDataUtil;
import MiniCash.util.ItemSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.block.ShulkerBox;
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
        String serverName = ItemSearch.getServerName();
        String worldName = player.getWorld().getName();
        String uuid = player.getUniqueId().toString();


        String inventoryContainerID = "PLAYER_INVENTORY_" + serverName + "_" + uuid;

        Map<String, ItemData> inventoryMap = parseInventoryContents(player.getInventory().getContents());

        ContainerModel invModel = new ContainerModel(
                inventoryContainerID, "PLAYER",
                player.getName(), uuid,
                serverName, worldName,
                null, null, null, inventoryMap
        );
        DatabaseManager.saveContainerData(invModel);

        String enderChestContainerID = "PLAYER_ENDERCHEST_" + uuid;
        Map<String, ItemData> enderchestItemMap = parseInventoryContents(player.getEnderChest().getContents());

        ContainerModel ecModel = new ContainerModel(
                enderChestContainerID, "ENDER_CHEST",
                player.getName(), uuid,
                serverName, worldName,
                null, null, null, enderchestItemMap
        );
        DatabaseManager.saveContainerData(ecModel);

    }


    /**
     * インベントリ内のアイテム（シュルカーボックス内包物含む）を解析して Map 化する共通処理
     */
    private Map<String, ItemData> parseInventoryContents(ItemStack[] contents) {
        Map<String, ItemData> itemMap = new HashMap<>();

        for (ItemStack item : contents) {
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
                    displayName = PlainTextComponentSerializer.plainText().serialize(
                            itemMeta.displayName() != null ? itemMeta.displayName() : itemMeta.itemName()
                    );
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

            // シュルカーボックス内部の解析
            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta instanceof BlockStateMeta blockStateMeta && blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
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
                            subDisplayName = PlainTextComponentSerializer.plainText().serialize(
                                    subMeta.displayName() != null ? subMeta.displayName() : subMeta.itemName()
                            );
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

        return itemMap;
    }

}
