package holy.moneybags.gui.mbags.items;

import holy.moneybags.Moneybags;
import holy.moneybags.storage.files.Configuration;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class StaticItem {
    @Getter
    private final Material material;
    @Getter
    private final int amount;
    @Getter
    private final String title;
    @Getter
    private final int lvl;

    public StaticItem(Material material, int amount, String title, final int lvl) {
        this.material = material;
        this.amount = amount;
        this.title = title;
        this.lvl = lvl;
    }

    /**
     * Возвращает элемент для меню
     * @param mission текущая миссия игрока в рамках уровня
     * @param progress количество сданных блоков
     * @return ItemStack элемент
     */
    public ItemStack build(final int mission, final int progress){
        final Configuration lang = Moneybags.getConfigurationManager().getConfig("lang.yml");

        final ItemStack stack = new ItemStack(material);
        final ItemMeta meta = stack.getItemMeta();
        assert meta != null;

        meta.setDisplayName(title);

        final List<String> staticItemLore = lang.cl("staticItemLore");
        final ArrayList<String> lore = new ArrayList<>();
        for (final String str: staticItemLore){
            lore.add(str.replace("%amount%",
                    String.valueOf(amount))
                    .replace("%progress%",String.valueOf(progress))
                    .replace("%remains%",String.valueOf(amount<=progress?0:amount-progress))
                    .replace("%mission%",String.valueOf(mission)));
        }
        meta.setLore(lore);

        stack.setItemMeta(meta);

        return stack;
    }
}
