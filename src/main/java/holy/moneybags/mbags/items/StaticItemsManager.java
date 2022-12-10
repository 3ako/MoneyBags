package holy.moneybags.mbags.items;

import holy.moneybags.Moneybags;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class StaticItemsManager {
    private final ArrayList<StaticItem> items = new ArrayList<StaticItem>();
    private final FileConfiguration itemsFile;

    public StaticItemsManager(final FileConfiguration itemsFile) {
        this.itemsFile = itemsFile;

        loadItems();
    }

    public StaticItem getItem(final Material material){
        for (final StaticItem item : items){
            if (item.getMaterial() == material)
                return item;
        }
        return null;
    }

    public List<StaticItem> getRandomItems(final int amount, final int lvl){
        ArrayList<StaticItem> randomItems = new ArrayList<StaticItem>();
        ArrayList<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < items.size(); i++){
            if (items.get(i).getLvl() == lvl)
                indexList.add(i);
        }

        for (int i = 0; i < amount; i++){
            int rnd = (int)(Math.random()*indexList.size());
            final StaticItem item = items.get(indexList.get(rnd));
            randomItems.add(item);

            indexList.remove(rnd);
        }

        return randomItems;
    }

    private void loadItems(){
        items.clear();
        try {
            final List<?> levels = itemsFile.getList("static-items");
            for (int lvl = 0; lvl < Objects.requireNonNull(levels).size(); lvl++){
                if (!(levels.get(lvl) instanceof ArrayList)) continue;

                final ArrayList<?> levelItems = (ArrayList<?>) levels.get(lvl);

                for (final Object j: levelItems){
                    if (!(j instanceof LinkedHashMap)) continue;

                    final LinkedHashMap levelItem = (LinkedHashMap) j;

                    final String   title    = ChatColor.translateAlternateColorCodes('&',(String) levelItem.get("title"));
                    final Material material = Material.getMaterial(((String) levelItem.get("material")).toUpperCase());
                    final   int    amount   = (int) levelItem.get("amount");
                    final StaticItem item   = new StaticItem(material,amount,title,lvl+1);

                    items.add(item);
                }
            }

            Moneybags.getInstance().getLogger().info("Loaded "+items.size()+" static items.");
        } catch (NullPointerException e){
            Moneybags.getInstance().getLogger().warning("Error loading static items, check config style");
        }

    }
}

