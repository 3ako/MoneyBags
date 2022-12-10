package holy.moneybags.mbags;

import holy.moneybags.Moneybags;
import holy.moneybags.mbags.items.DynamicItem;
import holy.moneybags.mbags.items.StaticItem;
import holy.moneybags.mbags.items.StaticItemsManager;
import holy.moneybags.mbags.items.DynamicItemsManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public class MBagsUser {
    @Getter
    private final UUID uuid;
    @Getter
    private int lvl = 1;
    @Getter
    private Player bukkitPlayer;
    @Getter
    private final ArrayList<ItemProgress> staticProgresses = new ArrayList<ItemProgress>();
    @Getter
    private final Map<Integer,DynamicItemProgress>  dynamicItemProgresses = new HashMap<>();

    @Getter @Setter
    private boolean dirty = false;
    public MBagsUser(UUID uuid, int lvl) {
        this.uuid = uuid;
        this.lvl = lvl;

        final Player player = Bukkit.getPlayer(uuid);
        if (player != null)
            bukkitPlayer = player;
    }


    public int getLvlMission(){
        int lvlMission = 0;
        final StaticItemsManager staticItemsManager = Moneybags.getInstance().getStaticItemsManager();
        for (final ItemProgress progress: getStaticActualStaticProgress()){
            final StaticItem item = staticItemsManager.getItem(progress.getMaterial());
            if (item.getAmount() <= progress.getProgress()) lvlMission++;
        }
        return lvlMission;
    }
    public void setLvl(int lvl){
        this.lvl = lvl;
        this.dirty = true;
    }
    public void resetStaticProgress() {
        final Iterator<ItemProgress> progressIterator = getStaticActualStaticProgress().iterator();
        while (progressIterator.hasNext()){
            final ItemProgress progress = progressIterator.next();
            progress.setDirty(ItemProgress.DirtyState.DELETE);
        }

        for (final StaticItem item: Moneybags.getInstance().getStaticItemsManager().getRandomItems(7,lvl)) {
            addStaticProgress(new ItemProgress(item.getMaterial(),0),true);
        }
    }

    public void generateDynamicItems(){
        final DynamicItemsManager dynamicItemsManager = Moneybags.getInstance().getDynamicItemsManager();
        final List<DynamicItem> generatedItems = dynamicItemsManager.generateRandomItems(lvl < 11?lvl:10);
        for (final DynamicItem item: generatedItems){
            final DynamicItemProgress progress = new DynamicItemProgress(item,0,item.getLvl());
            setDynamicProgress(progress,true);
        }

//        Bukkit.getScheduler().runTaskAsynchronously(Moneybags.getInstance(),()->{
//            Moneybags.getInstance().getStorageManager().getStorage().createDynamicItems(uuid,generatedItems);
//        });
    }
    public void generateDynamicItem(final int lvl){
        final DynamicItemsManager dynamicItemsManager = Moneybags.getInstance().getDynamicItemsManager();
        final DynamicItem item = dynamicItemsManager.getRandomItem(lvl);
        setDynamicProgress(item,0,true);
//        Bukkit.getScheduler().runTaskAsynchronously(Moneybags.getInstance(),()->{
//            // Кринж блять, но хочется спать
//            final ArrayList<DynamicItem> items = new ArrayList<>();
//            items.add(item);
//            Moneybags.getInstance().getStorageManager().getStorage().createDynamicItems(uuid,items);
//        });
    }
    public void setProgress(final Material material, final int count){
        for (final ItemProgress progress: getStaticActualStaticProgress()){
            if (progress.getMaterial() == material){
                progress.setProgress(count);
            }
        }
    }
    public void addProgress(final Material material, final int count){
        for (final ItemProgress progress: getStaticActualStaticProgress()){
            if (progress.getMaterial() == material){
                progress.setProgress(count+ progress.getProgress());
            }
        }
    }
    public void setDynamicProgress(final DynamicItemProgress item,final boolean isNew){
        if (isNew)
            item.setDirty(ItemProgress.DirtyState.CREATE);
        dynamicItemProgresses.put(item.getLvl(),item);
    }
    public void setDynamicProgress(final DynamicItem item, final int progress, final boolean isNew){
        final DynamicItemProgress dynamicItemProgress = new DynamicItemProgress(item,progress,item.getLvl());
        setDynamicProgress(dynamicItemProgress,isNew);
    }
    public ArrayList<ItemProgress> getStaticActualStaticProgress(){
        final ArrayList<ItemProgress> activeStaticProgresses = new ArrayList<>();

        for (final ItemProgress progress : staticProgresses){
            if (progress.getDirty() != ItemProgress.DirtyState.DELETE) {
                activeStaticProgresses.add(progress);
            }
        }

        return activeStaticProgresses;
    }

    public void addStaticProgress(final ItemProgress progress, final boolean isNew){
        if (isNew)
            progress.setDirty(ItemProgress.DirtyState.CREATE);
        else
            progress.setDirty(ItemProgress.DirtyState.ACTUAL);

        this.staticProgresses.add(progress);
    }
    public static class DynamicItemProgress extends ItemProgress {
        @Getter
        private final int lvl;
        @Getter
        private final DynamicItem item;
        public static int getMaxProgress(int userLvl){
            return userLvl * 640;
        }
        public DynamicItemProgress(DynamicItem item, int progress, int lvl) {
            super(item.getMaterial(), progress);
            this.item = item;
            this.lvl  = lvl;
        }
    }
    public static class ItemProgress {
        @Getter
        private final Material material;
        @Getter
        private int progress;

        @Getter @Setter
        private DirtyState dirty = DirtyState.ACTUAL;

        public ItemProgress(final Material material, final int progress) {
            this.material = material;
            this.progress = progress;
        }
        public void setProgress(final int progress){
            this.progress = progress;
            this.dirty = DirtyState.UPDATE;
        }

        public enum DirtyState {
            CREATE,
            ACTUAL,
            UPDATE,
            DELETE
        }
    }

}
