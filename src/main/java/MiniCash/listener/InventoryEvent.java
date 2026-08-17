package MiniCash.listener;

import MiniCash.Database.DatabaseManager;
import MiniCash.ItemSearch;
import MiniCash.model.ContainerModel;
import MiniCash.model.ItemData;
import MiniCash.util.CustomModelDataUtil;
import MiniCash.util.ItemSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class InventoryEvent implements Listener {

    private ItemSearch plugin;
    public InventoryEvent(ItemSearch plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event){
        if (event.getPlayer() instanceof Player player){


            savePlayerAndEnderChest(player);

            Inventory inventory = event.getInventory();

            InventoryHolder holder = inventory.getHolder();

            if (holder instanceof DoubleChest doubleChest) {
                Location location = doubleChest.getLocation(); // DoubleChest の中心座標
                String worldName = location.getWorld().getName();
                int x = location.getBlockX();
                int y = location.getBlockY();
                int z = location.getBlockZ();

                String containerId = "BLOCK_" + ItemSearch.getServerName() + "_" + worldName + "_" + x + "_" + y + "_" + z;
                String containerType = "CHEST";

                saveInventoryData(inventory, containerId, containerType, player, location);

            } else if (holder instanceof Container container) {
                saveContainerBlock(container, player);
            }




        }



    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        savePlayerAndEnderChest(event.getPlayer());
    }

    /**
     * プレイヤーインベントリとエンダーチェストの保存
     */
    private void savePlayerAndEnderChest(Player player) {
        String playerInventoryID = "PLAYER_INVENTORY_" + ItemSearch.getServerName() + "_" + player.getUniqueId();
        saveInventoryData(player.getInventory(), playerInventoryID, "PLAYER", player, player.getLocation());

        String enderChestID = "PLAYER_ENDERCHEST_" + player.getUniqueId();
        saveInventoryData(player.getEnderChest(), enderChestID, "ENDER_CHEST", player, player.getLocation());
    }


    /**
     * コンテナブロックの座標・ID情報を抽出して保存
     */
    private void saveContainerBlock(Container container, Player player) {
        Location location = container.getLocation();
        String worldName = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        String containerId = "BLOCK_" + ItemSearch.getServerName() + "_" + worldName + "_" + x + "_" + y + "_" + z;
        String containerType = container.getBlock().getType().name();

        saveInventoryData(container.getInventory(), containerId, containerType, player, location);
    }

    /**
     * インベントリ内のアイテムを解析・ハッシュ合算し保存するメソッド
     */
    private void saveInventoryData(Inventory inventory, String containerId, String containerType, Player player, Location loc) {
        if (inventory == null){
            return;
        }

        Map<String, ItemData> itemMap = new HashMap<>();

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            ItemStack hashItem = item.clone();
            hashItem.setAmount(1);

            String base64 = ItemSerializer.itemSerializer(item);
            String hashBase64 = ItemSerializer.itemSerializer(hashItem);
            String hash = ItemSerializer.getMD5Hash(hashBase64);

            if (itemMap.containsKey(hash)) {

                itemMap.get(hash).addAmount(item.getAmount());

            } else {

                ItemMeta itemMeta = item.getItemMeta();
                Integer customModelData = CustomModelDataUtil.getCustomModelData(itemMeta);
                String displayName = null;

                if(itemMeta != null) {
                    if (itemMeta.hasDisplayName()) {
                        displayName = PlainTextComponentSerializer.plainText().serialize(itemMeta.displayName());
                    }else if (itemMeta.hasItemName()) {
                        displayName = PlainTextComponentSerializer.plainText().serialize(itemMeta.itemName());
                    }

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

            // シュルカーボックスの中のアイテムまで処理
            ItemMeta itemMeta = item.getItemMeta();
            if (itemMeta instanceof BlockStateMeta blockStateMeta && blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
                for (ItemStack subItem : shulkerBox.getInventory().getContents()) {
                    if (subItem == null || subItem.getType().isAir()) {
                        continue;
                    }

                    ItemStack subHashItem = subItem.clone();
                    subHashItem.setAmount(1);

                    String subHashBase64 = ItemSerializer.itemSerializer(subHashItem);
                    String subHash = ItemSerializer.getMD5Hash(subHashBase64);

                    if (itemMap.containsKey(subHash)) {
                        itemMap.get(subHash).addAmount(subItem.getAmount());
                    } else {
                        ItemMeta subMeta = subItem.getItemMeta();
                        Integer subCustomModelData = CustomModelDataUtil.getCustomModelData(subMeta);
                        String subDisplayName = null;

                        if(subMeta != null) {
                            if (subMeta.hasDisplayName()) {
                                subDisplayName = PlainTextComponentSerializer.plainText().serialize(subMeta.displayName());
                            }else if (subMeta.hasItemName()) {
                                subDisplayName = PlainTextComponentSerializer.plainText().serialize(subMeta.itemName());
                            }

                        }

                        ItemData subItemData = new ItemData(
                                subHash,
                                subItem.getType().name(),
                                subItem.getAmount(),
                                null, // 検索専用のためBase64はnull
                                subCustomModelData,
                                subDisplayName,
                                1
                        );
                        itemMap.put(subHash, subItemData);
                    }
                }
            }
        }

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        String world = loc.getWorld().getName();

        ContainerModel containerModel = new ContainerModel(
                containerId, containerType, player.getName(), player.getUniqueId().toString(),
                ItemSearch.getServerName(), world, x, y, z, itemMap
        );

        DatabaseManager.saveContainerData(containerModel);

    }






}
