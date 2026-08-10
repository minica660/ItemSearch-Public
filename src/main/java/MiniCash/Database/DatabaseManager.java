package MiniCash.Database;

import MiniCash.ItemSearch;
import MiniCash.model.ContainerModel;
import MiniCash.model.ItemData;
import MiniCash.model.SearchResult;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Material;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {

    private static ItemSearch plugin;

    private static String URL;
    private static String USER;
    private static String PASSWORD;

    private static HikariDataSource hikariSe;

    public DatabaseManager(ItemSearch plugin) {
        DatabaseManager.plugin = plugin;

        URL = "jdbc:mysql://"
                + plugin.getConfig().getString("mysql.host")
                + ":" + plugin.getConfig().getInt("mysql.port")
                + "/" + plugin.getConfig().getString("mysql.database")
                + "?useSSL=true&autoReconnect=true&serverTimezone=Asia/Tokyo";
        USER = plugin.getConfig().getString("mysql.user");
        PASSWORD = plugin.getConfig().getString("mysql.password");

    }

    public static HikariDataSource getHikariSe() {
        return hikariSe;
    }

    public static void connect() {
        if (hikariSe != null && !hikariSe.isClosed()) {
            plugin.getLogger().warning("既にプールが存在します");
            return;
        }

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(URL);
        config.setUsername(USER);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(10);  // 接続数
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        try {
            //db接続
            hikariSe = new HikariDataSource(config);

            plugin.getLogger().info("Mysqlデータベースへの接続が完了しました");

            setupTable();

        } catch (Exception e) {

            plugin.getLogger().severe("Mysqlデータベースへの接続に失敗しました: " + e.getMessage());
//            plugin.getServer().getPluginManager().disablePlugin(plugin);
//            plugin.getLogger().severe("プラグインを停止します");

        }


    }

    public static void disConnect() {
        if (hikariSe != null && !hikariSe.isClosed()) {
            hikariSe.close();
            plugin.getLogger().info("データベースの接続プールの切断に成功しました");
        }
    }


    public static void setupTable() {

        String sql = """
                
                    CREATE TABLE IF NOT EXISTS `item_database` (
                    `id` BIGINT NOT NULL AUTO_INCREMENT,    
                    `container_id` VARCHAR(256) NOT NULL,    -- 例: "server1_world_100_64_-200" または "PLAYER_UUID"
                    `container_type` VARCHAR(64) NOT NULL,    -- "CHEST", "DROPPER", "ENDER_CHEST", "PLAYER" など
                    `item_hash` VARCHAR(32) NOT NULL,
                    `amount` INT NOT NULL,                    -- スタック数
                    `material` VARCHAR(64) NOT NULL,         -- Material名 (例: "DIAMOND_SWORD")
                    `custom_model_data` INT NULL,             -- CustomModelData
                    `is_nested` TINYINT DEFAULT 0,            -- 0: 通常・親アイテム / 1: シュルカーボックス等の内部アイテム
                    `display_name` VARCHAR(128) NULL,             -- 表示名
                    `item_data` LONGTEXT NULL,                  -- Data Component含むItemStackのBase64文字列
                    `server` VARCHAR(128) NOT NULL,           -- Bungee/Velocity等のサーバー識別名
                    `world` VARCHAR(128) NULL,                -- ワールド名
                    `x` INT NULL,                             -- X座標
                    `y` INT NULL,                             -- Y座標
                    `z` INT NULL,                             -- Z座標
                    `final_editor_name` VARCHAR(32) NULL,     -- 最後に操作したプレイヤー名
                    `final_editor_uuid` VARCHAR(64) NULL,     -- 最後に操作したプレイヤーUUID   
                    `last_date` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (`id`),
                    INDEX `idx_container` (`container_id`),
                    INDEX `idx_hash` (`item_hash`),
                    INDEX `idx_search` (`material`, `custom_model_data`),
                    INDEX `idx_editor` (`final_editor_uuid`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;


        String itemLogSQL = """
                
                    CREATE TABLE IF NOT EXISTS `item_log` (
                    `id` BIGINT NOT NULL AUTO_INCREMENT,
                    `final_editor_name` VARCHAR(64) NULL,
                    `final_editor_uuid` VARCHAR(64) NULL,
                    `item_hash` VARCHAR(32) NOT NULL,
                    `material` VARCHAR(64) NOT NULL,
                    `item_count` INT NOT NULL,
                    `custom_model_data` INT NULL,
                    `date_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (`id`),
                    INDEX `idx_log_search` (`material`, `date_time`),
                    INDEX `idx_log_user` (`final_editor_uuid`, `date_time`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """;

        try (Connection conn = hikariSe.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             PreparedStatement itemLogPstmt = conn.prepareStatement(itemLogSQL)) {
            stmt.executeUpdate();
            itemLogPstmt.executeUpdate();

            plugin.getLogger().info("item_databaseテーブルを作成しました");
            plugin.getLogger().info("item_logテーブルを作成しました");


        } catch (SQLException e) {

            plugin.getLogger().log(Level.SEVERE, "DBのテーブル生成中にエラーが発生しました", e);

        }


    }


    /**
     * コンテナの全アイテムデータをデータベースへ上書きし、保存する
     *
     * @param containerModel 保存対象の ContainerModel
     */
    public static void saveContainerData(ContainerModel containerModel) {

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {

            if (hikariSe == null || hikariSe.isClosed()) {

                plugin.getLogger().warning("DB接続が存在しないため保存処理を行いませんでした: [class:DatabaseManager...saveContainerData] ");
                return;
            }


            // 既存データは削除
            String deleteSql = "DELETE FROM `item_database` WHERE `container_id` = ?;";

            String insertSql = """
                    INSERT INTO `item_database` (
                        `container_id`, `container_type`, `item_hash`, `amount`, `material`,
                        `custom_model_data`, `is_nested`, `display_name`, `item_data`,
                        `server`, `world`, `x`, `y`, `z`, `final_editor_name`, `final_editor_uuid`
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;


            try (Connection conn = hikariSe.getConnection()) {

                conn.setAutoCommit(false);

                try {

                    try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                        deleteStmt.setString(1, containerModel.containerId());
                        deleteStmt.executeUpdate();
                    }

                    // データを新しく追加し 更新！
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        for (ItemData item : containerModel.items().values()) {
                            insertStmt.setString(1, containerModel.containerId());
                            insertStmt.setString(2, containerModel.containerType());
                            insertStmt.setString(3, item.getItemHash());
                            insertStmt.setInt(4, item.getAmount());
                            insertStmt.setString(5, item.getItemType());

                            // customModelData
                            if (item.getCustomModelData() != null) {
                                insertStmt.setInt(6, item.getCustomModelData());
                            } else {
                                insertStmt.setNull(6, Types.INTEGER);
                            }

                            insertStmt.setInt(7, item.getIsNested());

                            // displayName
                            if (item.getDisplayName() != null) {
                                insertStmt.setString(8, item.getDisplayName());
                            } else {
                                insertStmt.setNull(8, Types.VARCHAR);
                            }

                            // itemBase64
                            if (item.getItemBase64() != null) {
                                insertStmt.setString(9, item.getItemBase64());
                            } else {
                                insertStmt.setNull(9, Types.LONGVARCHAR);
                            }

                            insertStmt.setString(10, containerModel.server());

                            // world
                            if (containerModel.world() != null) {
                                insertStmt.setString(11, containerModel.world());
                            } else {
                                insertStmt.setNull(11, Types.VARCHAR);
                            }

                            // x,y,z
                            if (containerModel.x() != null) {
                                insertStmt.setInt(12, containerModel.x());
                            } else {
                                insertStmt.setNull(12, Types.INTEGER);
                            }

                            if (containerModel.y() != null) {
                                insertStmt.setInt(13, containerModel.y());
                            } else {
                                insertStmt.setNull(13, Types.INTEGER);
                            }

                            if (containerModel.z() != null) {
                                insertStmt.setInt(14, containerModel.z());
                            } else {
                                insertStmt.setNull(14, Types.INTEGER);
                            }

                            insertStmt.setString(15, containerModel.editorName());
                            insertStmt.setString(16, containerModel.editorUuid());

                            insertStmt.addBatch();
                        }


                        insertStmt.executeBatch();

                    }


                    conn.commit();

                } catch (SQLException e) {

                    conn.rollback();
                    plugin.getLogger().log(Level.SEVERE, "コンテナデータの保存処理中にエラーが発生したため、ロールバックしました: " + containerModel.containerId(), e);

                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "DB接続の取得中にエラーが発生しました", e);
            }


        });

    }


    /**
     * 指定されたcontainerIdのデータをDBから削除
     * @param containerId 削除対象のID
     */
    public static void deleteContainerData(String containerId) {

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {

            if (hikariSe == null || hikariSe.isClosed()) {
                return;
            }

            String sql = "DELETE FROM `item_database` WHERE `container_id` = ?;";

            try (Connection conn = hikariSe.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, containerId);
                stmt.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "コンテナデータの削除中にエラーが発生しました: " + containerId, e);
            }


        });

    }




    /**
     * 条件に応じたアイテムの検索
     */
    public static CompletableFuture<List<SearchResult>> searchItems(
            Material material, Integer customModelData, String displayName, String editor, String world, Integer nearX, Integer nearZ, Integer radius , String itemHash) {


        return CompletableFuture.supplyAsync(() -> {

            List<SearchResult> results = new ArrayList<>();

            if (hikariSe == null || hikariSe.isClosed()){

                return results;

            }

            StringBuilder sql = new StringBuilder("SELECT * FROM `item_database` WHERE 1=1 ");
            List<Object> params = new java.util.ArrayList<>();

            if (itemHash != null && !itemHash.isEmpty()) {

                sql.append("AND `item_hash` = ? ");
                params.add(itemHash);

            } else {

                if (material != null) {
                    sql.append("AND `material` = ? ");
                    params.add(material.name());
                }
                if (customModelData != null) {
                    sql.append("AND `custom_model_data` = ? ");
                    params.add(customModelData);
                }
                if (displayName != null && !displayName.isEmpty()) {
                    sql.append("AND `display_name` LIKE ? ");
                    params.add("%" + displayName + "%");
                }
            }

            if (editor != null && !editor.isEmpty()) {
                sql.append("AND (`final_editor_name` = ? OR `final_editor_uuid` = ?) ");
                params.add(editor);
                params.add(editor);
            }
            if (world != null && nearX != null && nearZ != null && radius != null) {
                sql.append("AND `world` = ? AND `x` BETWEEN ? AND ? AND `z` BETWEEN ? AND ? ");
                params.add(world);
                params.add(nearX - radius);
                params.add(nearX + radius);
                params.add(nearZ - radius);
                params.add(nearZ + radius);
            }

            sql.append("ORDER BY `last_date` DESC LIMIT 100;");

            try (Connection conn = hikariSe.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

                for (int i = 0; i < params.size(); i++) {
                    pstmt.setObject(i + 1, params.get(i));
                }

                try (ResultSet rs = pstmt.executeQuery()) {

                    while (rs.next()) {
                        String matStr = rs.getString("material");
                        org.bukkit.Material matEnum = org.bukkit.Material.matchMaterial(matStr);
                        if (matEnum == null) {
                            matEnum = org.bukkit.Material.AIR;
                        }

                        results.add(new MiniCash.model.SearchResult(
                                rs.getString("container_id"),
                                rs.getString("container_type"),
                                matEnum,
                                rs.getInt("amount"),
                                (Integer) rs.getObject("custom_model_data"),
                                rs.getString("display_name"),
                                rs.getInt("is_nested"),
                                rs.getString("server"),
                                rs.getString("world"),
                                (Integer) rs.getObject("x"),
                                (Integer) rs.getObject("y"),
                                (Integer) rs.getObject("z"),
                                rs.getString("final_editor_name"),
                                rs.getString("final_editor_uuid"),
                                rs.getTimestamp("last_date")
                        ));
                    }

                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "アイテム検索中にエラーが発生しました", e);
            }
            return results;
        });


    }



    /**
     * 定期的にサーバー全体の特定のアイテム保有状況を集計し、item_logへ記録
     * * @param minCount ログに記録する最小個数のしきい値 (例: 5個以上)
     */
    public static void logWholeServerItemCount(int minCount) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (hikariSe == null || hikariSe.isClosed()) {
                return;
            }

            String sql = """
            INSERT INTO item_log (
                `final_editor_name`, `final_editor_uuid`, `item_hash`, `material`,
                `item_count`, `custom_model_data`, `date_time`
            )
            SELECT 
                `final_editor_name`, `final_editor_uuid`, `item_hash`, `material`,
                SUM(`amount`) as `item_count`, `custom_model_data`, NOW()
            FROM `item_database`
            WHERE `final_editor_uuid` IS NOT NULL
            GROUP BY `final_editor_uuid`, `final_editor_name`, `item_hash`, `material`, `custom_model_data`
            HAVING `item_count` >= ?
            ORDER BY `item_count` DESC;
        """;

            try (Connection conn = hikariSe.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, minCount);
                int rowsAffected = pstmt.executeUpdate();

                plugin.getLogger().info("定期集計ログを作成しました　記録件数: " + rowsAffected + "件");

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "item_logへの定期集計記録中にエラーが発生しました", e);
            }
        });
    }



}
