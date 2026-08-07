package MiniCash.model;

public class OldItemData {

    public String containerId;
    public String finalEditorName;  // 最後に操作したプレイヤー名
    public String finalEditorUUID;  // 最後に操作したプレイヤーUUID
    public String containerType;    // "CHEST", "DROPPER", "ENDER_CHEST", "PLAYER" など
    public int amount;              // スタック数
    public String server;
    public String world;
    public int x;
    public int y;
    public int z;
    public String itemBase64; // Data Component保持のBase64文字列
    public String itemType;
    public String dateTime;
    public Integer customModelData;

    public OldItemData(String finalEditorName, String finalEditorUUID, String containerType,
                       String containerId, int amount, String server,
                       String world, int x, int y, int z, String itemBase64, String itemType , String dateTime , Integer customModelData) {
        this.finalEditorName = finalEditorName;
        this.finalEditorUUID = finalEditorUUID;
        this.containerType = containerType;
        this.containerId = containerId;
        this.amount = amount;
        this.server = server;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.itemBase64 = itemBase64;
        this.itemType = itemType;
        this.dateTime = dateTime;
        this.customModelData = customModelData;
    }

    public String getFormattedLocation() {

        if (world == null) {
            return "Unknown";
        }

        return world + " (" + x + ", " + y + ", " + z + ")";

    }

}
