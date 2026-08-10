package MiniCash.listener;

import MiniCash.Database.DatabaseManager;
import MiniCash.ItemSearch;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.StructureGrowEvent;

public class BlockUpdateEvent implements Listener {

    private final ItemSearch plugin;
    public BlockUpdateEvent(ItemSearch plugin) {
        this.plugin = plugin;
    }


    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        handleBlockRemove(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            handleBlockRemove(block);
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            handleBlockRemove(block);
        }
    }




    /**
     * ブロックがコンテナであればDBからレコードを削除するメソッドです
     */
    private void handleBlockRemove(Block block) {

        if (!(block.getState() instanceof Container)) {
            return;
        }

        Location location = block.getLocation();
        String worldName = block.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        String containerId = "BLOCK_" + ItemSearch.getServerName() + "_" + worldName + "_" + x + "_" + y + "_" + z;

        DatabaseManager.deleteContainerData(containerId);

    }



}
