package holy.moneybags.mbags.items;

import holy.moneybags.Moneybags;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class DynamicItemsManager {

    private final FileConfiguration itemsFile;
    private final Map<Integer,ArrayList<DynamicItem>> items = new HashMap<>();
    public DynamicItemsManager(final FileConfiguration itemsFile) {
        this.itemsFile = itemsFile;
        loadItems();
    }

    /**
     * Возвращает случайный динамический предмет
     * @param lvl уровень необходимого предмета
     */
    public DynamicItem getRandomItem(final int lvl){
        if (!items.containsKey(lvl)) return null;
        final ArrayList<DynamicItem> levelItems = items.get(lvl);
        final int rnd = (int)(Math.random()*levelItems.size());
        return levelItems.get(rnd);
    }

    /**
     * Генерирует карту случайных динамических предметов
     * @param lvl до какого уровня генерируем (По сути - количество)
     * @return List<DynamicItem> Список предметов
     */
    public List<DynamicItem> generateRandomItems(final int lvl) {
        final ArrayList<DynamicItem> generatedItems = new ArrayList<DynamicItem>();
        for (int i = 1; i<=lvl; i++){
            final DynamicItem item = getRandomItem(i);
            generatedItems.add(item);
        }
        return generatedItems;
    }


    /**
     * Собирает динамическе предметы из файла
     */
    private void loadItems(){
        items.clear();
        try {
            final List<?> levels = itemsFile.getList("dynamic-items");
            for (int lvl = 0; lvl < Objects.requireNonNull(levels).size(); lvl++){
                if (!(levels.get(lvl) instanceof ArrayList)) continue;

                final ArrayList<?> levelItems = (ArrayList<?>) levels.get(lvl);

                for (final Object j: levelItems){
                    if (!(j instanceof LinkedHashMap)) continue;

                    final LinkedHashMap levelItem = (LinkedHashMap) j;

                    final String   title    = ChatColor.translateAlternateColorCodes('&',(String) levelItem.get("title"));
                    final Material material = Material.getMaterial(((String) levelItem.get("material")).toUpperCase());
                    final  int startPrice   = (int) levelItem.get("startPrice");
                    final  int  priceStep   = (int) levelItem.get("priceStep");
                    final DynamicItem item  = new DynamicItem(material,startPrice,priceStep,title,lvl+1);

                    if (!items.containsKey(lvl+1))
                        items.put(lvl+1,new ArrayList<>());
                    items.get(lvl+1).add(item);

                }
            }

            Moneybags.getInstance().getLogger().info("Loaded "+items.size()+" dynamic items.");
        } catch (NullPointerException e){
            Moneybags.getInstance().getLogger().warning("Error loading dynamic items, check config style");
        }

    }

    /**
     * Получить объект предмета
     * @param material
     * @param lvl
     */
    public DynamicItem getItem(final Material material, final int lvl){
        if (!items.containsKey(lvl)) return null;

        for (final DynamicItem item : items.get(lvl)){
            if (item.getMaterial() == material) return item;
        }

        return null;
    }
}
