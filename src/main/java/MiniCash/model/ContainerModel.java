package MiniCash.model;

import java.util.Map;

public record ContainerModel(
        String containerId,
        String containerType,
        String editorName,
        String editorUuid,
        String server,
        String world,
        Integer x,
        Integer y,
        Integer z,
        Map<String, ItemData> items
) {}

