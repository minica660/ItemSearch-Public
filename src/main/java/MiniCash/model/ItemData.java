package MiniCash.model;

public class ItemData {

    public String itemHash;         // MD5ハッシュ
    public String itemType;         // Material
    public int amount;
    public String itemBase64;       // Data Component保持のBase64
    public Integer customModelData; // カスタムモデルデータ
    public String displayName;      // アイテムの表示名
    public int isNested;            // シュルカー中身判定 (0: 通常, 1: 中身)

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


    public void addAmount(int add) {
        this.amount = this.amount + add;
    }

}
