package MiniCash.model;

public class ItemData {

    private final String itemHash;         // MD5ハッシュ
    private final String itemType;         // Material名
    private int amount;                    // スタック数（加算可能）
    private final String itemBase64;       // Data Component保持のBase64
    private final Integer customModelData; // カスタムモデルデータ
    private final String displayName;      // アイテムの表示名
    private final int isNested;            // シュルカー中身判定 (0: 通常, 1: 中身)

    // コンストラクタ
    public ItemData(String itemHash, String itemType, int amount, String itemBase64,
                    Integer customModelData, String displayName, int isNested) {
        this.itemHash = itemHash;
        this.itemType = itemType;
        this.amount = amount;
        this.itemBase64 = itemBase64;
        this.customModelData = customModelData;
        this.displayName = displayName;
        this.isNested = isNested;
    }

    // 数量の加算メソッド
    public void addAmount(int add) {
        this.amount += add;
    }

    // Getter メソッド
    public String getItemHash() {
        return itemHash;
    }

    public String getItemType() {
        return itemType;
    }

    public int getAmount() {
        return amount;
    }

    public String getItemBase64() {
        return itemBase64;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getIsNested() {
        return isNested;
    }

}
