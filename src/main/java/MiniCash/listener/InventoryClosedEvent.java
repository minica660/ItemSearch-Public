package MiniCash.listener;

import MiniCash.Database.DatabaseManager;
import MiniCash.ItemSearch;
import MiniCash.model.ContainerModel;
import MiniCash.model.ItemData;
import MiniCash.util.CustomModelDataUtil;
import MiniCash.util.ItemSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.HashMap;
import java.util.Map;

public class InventoryClosedEvent implements Listener {

    private ItemSearch plugin;
    public InventoryClosedEvent(ItemSearch plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event){
        if (event.getPlayer() instanceof Player player){


            Inventory inventory = event.getInventory();
            String containerType;
            String containerId;
            String world = player.getWorld().getName();

            int x;
            int y;
            int z;

            if (inventory.getHolder() instanceof Container container) {

                containerType = container.getBlock().getType().name();
                Location loc = container.getLocation();
                x = loc.getBlockX();
                y = loc.getBlockY();
                z = loc.getBlockZ();

                containerId = "BLOCK_" + ItemSearch.getServerName() + "_" + world + "_" + x + "_" + y + "_" + z;

            } else if (event.getView().getType() == InventoryType.ENDER_CHEST) {

                containerType = "ENDER_CHEST";

                Location location = event.getPlayer().getLocation();
                x = location.getBlockX();
                y = location.getBlockY();
                z = location.getBlockZ();

                containerId = "PLAYER_ENDERCHEST_" + player.getUniqueId();

            } else if (event.getView().getType() == InventoryType.CRAFTING || event.getView().getType() == InventoryType.PLAYER) {

                containerType = "PLAYER";

                Location location = event.getPlayer().getLocation();
                x = location.getBlockX();
                y = location.getBlockY();
                z = location.getBlockZ();

                containerId = "PLAYER_INVENTORY_" + ItemSearch.getServerName() + "_" + player.getUniqueId();

            } else {
                return;
            }


            Map<String, ItemData> itemMap = new HashMap<>();

            for (ItemStack item : inventory.getContents()) {

                if (item == null || item.getType().isAir()){
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


                            if(subMeta != null && subMeta.hasDisplayName()) {
                                subDisplayName = PlainTextComponentSerializer.plainText().serialize(subMeta.itemName());

                            }
                            // 検索専用だからBase64はnullに設定
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
                    containerId, containerType, player.getName(), player.getUniqueId().toString(),
                    ItemSearch.getServerName(), world, x, y, z, itemMap
            );


            // DB保存処理
            DatabaseManager.saveContainerData(containerModel);






        }



    }






}
