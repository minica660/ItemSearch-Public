package MiniCash.Database;

import MiniCash.ItemSearch;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
                PRIMARY KEY (`container_id`, `material`, `item_hash`),
                    INDEX `idx_search` (`material`, `custom_model_data`),
                    INDEX `idx_editor` (`final_editor_uuid`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """;


        String itemLogSQL = """
            
                CREATE TABLE IF NOT EXISTS `item_log` (
                `id` BIGINT NOT NULL AUTO_INCREMENT,
                `final_editor_name` VARCHAR(64) NULL,
                `final_editor_uuid` VARCHAR(64) NULL,
                `material` VARCHAR(64) NOT NULL,
                `item_count` INT NOT NULL,
                `custom_model_data` INT NULL,
                `date_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (`id`),
                INDEX `idx_log_search` (`material`, `date_time`)
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

            plugin.getLogger().log(Level.SEVERE,"DBのテーブル生成中にエラーが発生しました",e);

        }



    }


}
