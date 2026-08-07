package MiniCash.Database;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class ItemRepository {
    private final DatabaseManager dbManager;
    private HikariDataSource dataSource;

    public ItemRepository(DatabaseManager dbManager,HikariDataSource dataSource) {
        this.dbManager = dbManager;
        this.dataSource = dataSource;
    }

    // 特定のコンテナ/プレイヤーのデータを一括更新 (UPSERT/置換)
    public CompletableFuture<Void> saveItemsAsync(String ownerType, String ownerId, List<ItemStack> items) {

        return CompletableFuture.runAsync(() -> {

            // 古いデータを削除
            String deleteSql = "DELETE FROM item_records WHERE owner_id = ?";
            // 新しいアイテム群をBatch挿入 (Prepared Statement + addBatch)
            String insertSql = "INSERT INTO item_records (owner_type, owner_id, material, display_name, amount, item_base64) VALUES (?, ?, ?, ?, ?, ?)";

            try (Connection conn = dataSource.getConnection()) {

                conn.setAutoCommit(false);

                try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                    deleteStmt.setString(1, ownerId);
                    deleteStmt.executeUpdate();
                }

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    for (ItemStack item : items) {
                        if (item == null || item.getType().isAir()) continue;

                        insertStmt.setString(1, ownerType);
                        insertStmt.setString(2, ownerId);
                        insertStmt.setString(3, item.getType().name());
                        insertStmt.setString(4, item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : null);
                        insertStmt.setInt(5, item.getAmount());
                        insertStmt.setString(6, itemToBase64(item)); // NBTシリアライズ処理
                        insertStmt.addBatch();
                    }
                    insertStmt.executeBatch();
                }

                conn.commit(); //
            } catch (SQLException e) {
                e.printStackTrace();
            }


        });
    }
}
