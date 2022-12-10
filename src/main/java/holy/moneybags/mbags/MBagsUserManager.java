package holy.moneybags.mbags;

import holy.moneybags.Moneybags;
import holy.moneybags.mbags.items.StaticItem;
import holy.moneybags.mbags.items.StaticItemsManager;
import holy.moneybags.storage.Storage;
import holy.moneybags.storage.files.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MBagsUserManager implements Listener {
    private final ArrayList<MBagsUser> users = new ArrayList<>();
    private final Storage storage;
    private final JavaPlugin plugin;
    private final StaticItemsManager staticItemsManager;
    private long  resetSchedulerStartTime = System.currentTimeMillis();
    final long resetDelay = TimeUnit.HOURS.toMillis(8);
    private Timer resetTimer = new Timer();

    public MBagsUserManager(final JavaPlugin plugin, final Storage storage, final StaticItemsManager staticItemsManager) {
        this.plugin  = plugin;
        this.storage = storage;
        this.staticItemsManager = staticItemsManager;
        for (final Player player: Bukkit.getOnlinePlayers()){
            addUser(player);
        }
        plugin.getServer().getPluginManager().registerEvents(this,plugin);
        runSaveScheduler();
    }
    public void removeAllDynamicStatistics(){
        storage.deleteAllDynamicItems();
        for (final MBagsUser user: users){
            user.getDynamicItemProgresses().clear();
            user.generateDynamicItems();
        }
    }
    public MBagsUser getUser(final Player player){
        for (final MBagsUser user : users){
            if (user.getBukkitPlayer() == player) return user;
        }
        return null;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event){
        addUser(event.getPlayer());
    }
    private void addUser(final Player player){
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            MBagsUser user = storage.getUser(player.getUniqueId());
            if (user == null){
                user = storage.createUser(player.getUniqueId());
            }

            if (user.getLvl() < 11 && user.getStaticProgresses().size() < 7){
                final List<StaticItem> newStaticItems = staticItemsManager.getRandomItems(7, user.getLvl());
                for (final StaticItem item : newStaticItems){
                    final MBagsUser.ItemProgress progress = new MBagsUser.ItemProgress(item.getMaterial(), 0);
                    user.addStaticProgress(progress, true);
                }
            }

            if (user.getDynamicItemProgresses().size() < (user.getLvl()<11?user.getLvl():10)){
                user.generateDynamicItems();
            }

            users.add(user);
        });
    }
    @EventHandler
    private void onPluginDisabled(final PluginDisableEvent event) {
        resetTimer.cancel();
    }
    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event){
        for (final MBagsUser user : users){
            if (user.getBukkitPlayer() == event.getPlayer()){
                if (user.isDirty()) {
                    storage.saveUser(user);
                    users.remove(user);
                    return;
                }
            }
        }
    }

    public long getTimeToDynamicReset() {
        return resetDelay - (System.currentTimeMillis() - resetSchedulerStartTime);
    }
    private void runSaveScheduler(){
        final Configuration lang = Moneybags.getConfigurationManager().getConfig("lang.yml");
        final Configuration config = Moneybags.getConfigurationManager().getConfig("config.yml");

        final int delay = config.getFile().getInt("storage.mysql.updateDelay");
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,()->{
            for (final MBagsUser user : users){
                if (user.isDirty()){
                    storage.saveUser(user);
                    user.setDirty(false);
                }

                final ArrayList<MBagsUser.ItemProgress> removeProgresses = new ArrayList<>();
                final ArrayList<MBagsUser.ItemProgress> createProgresses = new ArrayList<>();
                final ArrayList<MBagsUser.ItemProgress> updateProgresses = new ArrayList<>();

                final Iterator<MBagsUser.ItemProgress> staticProgressIterator = user.getStaticProgresses().iterator();
                while (staticProgressIterator.hasNext()){
                    final MBagsUser.ItemProgress progress = staticProgressIterator.next();
                    switch (progress.getDirty()){
                        case CREATE:
                            createProgresses.add(progress);
                            progress.setDirty(MBagsUser.ItemProgress.DirtyState.ACTUAL);
                            break;
                        case DELETE:
                            removeProgresses.add(progress);
                            staticProgressIterator.remove();
                            break;
                        case UPDATE:
                            updateProgresses.add(progress);
                            progress.setDirty(MBagsUser.ItemProgress.DirtyState.ACTUAL);
                            break;
                    }
                }

                if (createProgresses.size() > 0)
                    storage.createUserStaticStatistics(user.getUuid(),createProgresses);
                if (removeProgresses.size() > 0)
                    storage.deleteUserStaticStatistics(user.getUuid(),removeProgresses);
                if (updateProgresses.size() > 0)
                    storage.updateUserStaticStatistics(user.getUuid(),updateProgresses);

                final ArrayList<MBagsUser.DynamicItemProgress> createDProgresses = new ArrayList<>();
                final ArrayList<MBagsUser.DynamicItemProgress> updateDProgresses = new ArrayList<>();

                for(Iterator<Map.Entry<Integer, MBagsUser.DynamicItemProgress>> it = user.getDynamicItemProgresses().entrySet().iterator(); it.hasNext(); ) {
                    final Map.Entry<Integer, MBagsUser.DynamicItemProgress> entry = it.next();
                    final MBagsUser.DynamicItemProgress progress = entry.getValue();
                    switch (progress.getDirty()){
                        case CREATE:
                            createDProgresses.add(progress);
                            progress.setDirty(MBagsUser.ItemProgress.DirtyState.ACTUAL);
                            break;
                        case UPDATE:
                            updateDProgresses.add(progress);
                            progress.setDirty(MBagsUser.ItemProgress.DirtyState.ACTUAL);
                            break;
                    }
                }

                if (createDProgresses.size()>0)
                    storage.createDynamicItems(user.getUuid(),createDProgresses);
                if (updateDProgresses.size()>0)
                    storage.updateDynamicItems(user.getUuid(),updateDProgresses);

            }
        }, delay, delay);

        resetSchedulerStartTime = System.currentTimeMillis();
        resetTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                removeAllDynamicStatistics();
                for (final Player player : Bukkit.getOnlinePlayers()){
                    player.sendMessage(lang.c("dynamicItemsReset"));
                }
                resetSchedulerStartTime = System.currentTimeMillis();
            }
        }, 0, resetDelay);
    }

}
