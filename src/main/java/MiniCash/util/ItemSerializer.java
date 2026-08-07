package MiniCash.util;

import MiniCash.ItemSearch;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Level;

public class ItemSerializer {

    private static ItemSearch plugin;

    public ItemSerializer(ItemSearch plugin) {
        ItemSerializer.plugin = plugin;
    }

    /**
     * ItemStackをBase64 文字列に変換
     */
    public static String itemSerializer(ItemStack item) {
        if (item == null || item.getType().isAir()){
            return null;
        }

        byte[] bytes = item.serializeAsBytes();
        return Base64.getEncoder().encodeToString(bytes);

    }

    /**
     * Base64 文字列から ItemStackに変換
     */
    public static ItemStack deitemSerializer(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            // コンポーネント構造を崩さずに復元
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Base64文字列から32文字のMD5ハッシュを生成
     * 同じアイテム化の確認用
     */
    public static String getMD5Hash(String base64String) {
        if (base64String == null) return "null_hash";

        try {

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(base64String.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
             plugin.getLogger().log(Level.SEVERE,"MD5アルゴリズムが見つかりませんでした", e);
             return "null_hash";
        }
    }

}
