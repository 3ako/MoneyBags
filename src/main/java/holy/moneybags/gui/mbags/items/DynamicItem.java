package holy.moneybags.gui.mbags.items;

import holy.moneybags.Moneybags;
import holy.moneybags.Utils;
import holy.moneybags.storage.files.Configuration;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DynamicItem {
    @Getter
    private final Material material;
    @Getter
    private final int priceStep;
    @Getter
    private final int startPrice;
    @Getter
    private final String title;
    @Getter
    private final int lvl;

    public DynamicItem(final Material material, final int startPrice, final int priceStep, final String title, final int lvl) {
        this.title      = title;
        this.material   = material;
        this.priceStep  = priceStep;
        this.startPrice = startPrice;
        this.lvl        = lvl;
    }

    public ItemStack build(final int progress, final long time, final int userLvl){
        final Configuration lang = Moneybags.getConfigurationManager().getConfig("lang.yml");

        final ItemStack stack = new ItemStack(material);
        final ItemMeta meta = stack.getItemMeta();
        assert meta != null;

        meta.setDisplayName(title);

        final List<String> staticItemLore = lang.cl("dynamicItemLore");
        final ArrayList<String> lore = new ArrayList<>();
        for (final String str: staticItemLore){
            lore.add(str
                    .replace("%price%", String.valueOf(getPrice(lvl,time)))
                    .replace("%progress%",String.valueOf(progress)+"/"+userLvl*640)
                    .replace("%time%",String.format("%d ч., %d м., %d с.",
                            TimeUnit.MILLISECONDS.toHours(time),
                            TimeUnit.MILLISECONDS.toMinutes(time) -
                                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(time)),
                            TimeUnit.MILLISECONDS.toSeconds(time) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time))
                    ))
                    .replace("%priceStep%",String.valueOf(priceStep)));
        }
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }


    public double getPrice(int lvl, long time){
        final double min = 8*60 - TimeUnit.MILLISECONDS.toMinutes(time);
        final double fullPrice = startPrice+priceStep*(lvl-1);
        return Utils.round((fullPrice - (fullPrice/(8*60))*min),2);
    }


}
