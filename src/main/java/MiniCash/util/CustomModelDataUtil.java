package MiniCash.util;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

public class CustomModelDataUtil {

    public static Integer getCustomModelData(ItemMeta itemMeta){

        if(itemMeta == null){
            return null;
        }

        if (itemMeta.hasCustomModelDataComponent()) {
            CustomModelDataComponent customModelDataComponent = itemMeta.getCustomModelDataComponent();
            if (!customModelDataComponent.getFloats().isEmpty()) {

                return customModelDataComponent.getFloats().get(0).intValue();

            }
        }

        return null;
    }

}
