package MiniCash.model;

import org.bukkit.Material;

import java.sql.Timestamp;


public record SearchResult(
        String containerId,
        String containerType,
        Material material,
        int amount,
        Integer customModelData,
        String displayName,
        int isNested,
        String server,
        String world,
        Integer x,
        Integer y,
        Integer z,
        String finalEditorName,
        String finalEditorUuid,
        Timestamp lastDate
) {}
