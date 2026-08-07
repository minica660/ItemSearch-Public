package MiniCash.listener;

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
        Block block = event.getBlock();

        // チェストやドロッパーなどのコンテナブロックか判定
        if (block.getState() instanceof Container container) {
            Location location = block.getLocation();
            String world = location.getWorld().getName();
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();

            // 削除用の container_id を生成
            String containerId = "BLOCK_" + plugin.getServer().getName() + "_" + world + "_" + x + "_" + y + "_" + z;

            // DB から DELETE 実行


        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {

    }


    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
    }

    @EventHandler(ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
    }


}
