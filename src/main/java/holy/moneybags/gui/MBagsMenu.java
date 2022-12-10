package holy.moneybags.gui;

import holy.moneybags.Moneybags;
import holy.moneybags.Utils;
import holy.moneybags.mbags.MBagsUser;
import holy.moneybags.mbags.MBagsUserManager;
import holy.moneybags.mbags.items.StaticItem;
import holy.moneybags.mbags.items.StaticItemsManager;
import holy.moneybags.storage.files.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class MBagsMenu implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID,Inventory> viewing = new HashMap<>();
    private final MBagsUserManager mBagsUserManager;
    private static final ItemStack exit = new ItemStack(Material.BARRIER);
    private static final ItemStack emerald = new ItemStack(Material.EMERALD_BLOCK);
    static {
        ItemMeta meta = exit.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.RED+"Выход");
        exit.setItemMeta(meta);

        meta = emerald.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN+"Вы достигли максимального уровня");
        emerald.setItemMeta(meta);
    }
    private final Sound levelUpSound;
    private final Sound successSellSound;
    private final Sound failureSellSound;
    private final Permission battlePassPermission;
    private final List<String> levelUpRewardBattlePassCommands;
    private final List<String> levelUpRewardCommands;

    public MBagsMenu(final JavaPlugin plugin, final MBagsUserManager userManager){
        this.plugin = plugin;
        this.mBagsUserManager = userManager;
        plugin.getServer().getPluginManager().registerEvents(this,plugin);

        final Configuration config = Moneybags.getConfigurationManager().getConfig("config.yml");

        levelUpSound     = Sound.valueOf(config.c("levelUpSound").toUpperCase());
        successSellSound = Sound.valueOf(config.c("successSellSound").toUpperCase());
        failureSellSound = Sound.valueOf(config.c("failureSellSound").toUpperCase());

        // Rewards
        battlePassPermission = new Permission(config.c("battlePassPermission"));
        levelUpRewardBattlePassCommands = config.cl("levelUpRewardBattlePassCommands");
        levelUpRewardCommands = config.cl("levelUpRewardCommands");

        Bukkit.getScheduler().runTaskTimer(plugin,()->{
            updateAll();
        },20,20);
    }

    public void open(final MBagsUser user) {
        user.getBukkitPlayer().closeInventory();

        final Inventory inventory = Bukkit.createInventory(user.getBukkitPlayer(),54,"Толстосум (Уровень "+user.getLvl()+")");
        build(user,inventory);
        viewing.put(user.getUuid(),inventory);
        user.getBukkitPlayer().openInventory(inventory);

    }
    public void update(final MBagsUser user){
        final Inventory inventory = viewing.get(user.getUuid());
        if (inventory != null) build(user,inventory);
    }
    public void updateAll(){
        for (final Map.Entry<UUID,Inventory> entry : viewing.entrySet()){
            final Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;
            final MBagsUser user = mBagsUserManager.getUser(p);
            if (user == null) continue;
            update(user);
        }
    }
    public void build(final MBagsUser user, final Inventory inventory) {
        inventory.setItem(53,exit);

        final StaticItemsManager staticItemsManager = Moneybags.getInstance().getStaticItemsManager();
        final ArrayList<MBagsUser.ItemProgress> statistics = user.getStaticActualStaticProgress();
        if (user.getLvl() > 10){
            for (int i = 0; i < 7; i++){
                inventory.setItem(i+10, emerald);
            }
        } else
            for (int i = 0; i < statistics.size(); i++){
                final MBagsUser.ItemProgress progress = statistics.get(i);

                final StaticItem staticItem = staticItemsManager.getItem(progress.getMaterial());
                inventory.setItem(i+10,staticItem.build(user.getLvlMission(),progress.getProgress()));
            }

        for (int i = 1; i <= user.getDynamicItemProgresses().size(); i++){
            final MBagsUser.DynamicItemProgress item = user.getDynamicItemProgresses().get(i);
            inventory.setItem(28+(i<6?i:i+4),item.getItem().build(item.getProgress(),
                    mBagsUserManager.getTimeToDynamicReset(), user.getLvl()));
        }

        for (int i = user.getLvl(); i < 10; i++){
            final ItemStack item = new ItemStack(Material.RED_MUSHROOM_BLOCK);
            final ItemMeta meta = item.getItemMeta();
            assert meta != null;
            meta.setDisplayName(ChatColor.RED+"Откроется на "+(i+1)+" уровне");
            item.setItemMeta(meta);
            inventory.setItem(29+(i<5?i:i+4),item);
        }

        user.getBukkitPlayer().updateInventory();
    }

    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        final Player p = (Player) event.getWhoClicked();

        if (viewing.get(p.getUniqueId()) != event.getInventory()) return;

        event.setCancelled(true);

        final int slot = event.getSlot();
        if (slot == 53){
            p.closeInventory();
            return;
        }

        if (slot > 9 && slot < 17 ){
            final ItemStack currentItem = event.getCurrentItem();
            sellStaticItem(p,currentItem.getType());
            return;
        }

        if ((slot > 28 && slot < 34) || (slot > 37 && slot < 43)) {
            final ItemStack currentItem = event.getCurrentItem();
            sellDynamicItem(p,currentItem.getType(),slot);
            return;
        }


    }

    private void sellStaticItem(final Player player, final Material material){
        final MBagsUser user = mBagsUserManager.getUser(player);
        if (user == null) {
            player.playSound(player.getLocation(), failureSellSound, 1.0F, 1.0F);
            player.sendMessage("Произошла ошибка. Сессия не найдена, обратитесь к администрации");
            return;
        }
        if (user.getLvl() > 10 ) return;
        final Inventory inventory = player.getInventory();
        if (!inventory.contains(material)) {
            player.playSound(player.getLocation(), failureSellSound, 1.0F, 1.0F);
            player.sendMessage("Недостаточно предметов в инвентаре");
            return;
        }
        int count = 0;
        for (final ItemStack itemStack: inventory.getContents()){
            if (itemStack == null || itemStack.getType() != material) continue;
            count += itemStack.getAmount();
            inventory.remove(itemStack);
        }

        user.addProgress(material,count);
        final StaticItemsManager staticItemsManager = Moneybags.getInstance().getStaticItemsManager();
        final StaticItem staticItem = staticItemsManager.getItem(material);
        player.sendMessage("Вы успешно сдали "+count+" "+staticItem.getTitle());
        player.playSound(player.getLocation(), successSellSound, 1.0F, 1.0F);
        if (user.getLvlMission() >= 6){
            player.sendMessage("Вы выполнили 6 миссий и переходите на следующий уровень!");
            player.playSound(player.getLocation(), levelUpSound, 1.0F, 1.0F);
            user.setLvl(user.getLvl() + 1);

            // Sent rewards
            if (player.hasPermission(battlePassPermission)){
                for (final String cmd: levelUpRewardBattlePassCommands){
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),cmd.replace("%username%",player.getName()));
                }
            } else
                for (final String cmd: levelUpRewardCommands){
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),cmd.replace("%username%",player.getName()));
                }

            if (user.getLvl() < 11) {
                user.generateDynamicItem(user.getLvl());
                user.resetStaticProgress();
            }

        }

        update(user);
    }
    private void sellDynamicItem(final Player player, final Material material, final int slot){
        final MBagsUser user = mBagsUserManager.getUser(player);
        if (user == null) {
            player.playSound(player.getLocation(), failureSellSound, 1.0F, 1.0F);
            player.sendMessage("Произошла ошибка. Сессия не найдена, обратитесь к администрации");
            return;
        }

        int levelClick;
        if (slot < 34)
            levelClick = slot - 28;
        else
            levelClick = slot - 32;

        final MBagsUser.DynamicItemProgress progress = user.getDynamicItemProgresses().get(levelClick);
        if (progress == null || material != progress.getMaterial()) return;

        final Inventory inventory = player.getInventory();
        if (!inventory.contains(material)) {
            player.playSound(player.getLocation(), failureSellSound, 1.0F, 1.0F);
            player.sendMessage("Недостаточно предметов в инвентаре");
            return;
        }

        int count = 0;
        final int maxProgress = MBagsUser.DynamicItemProgress.getMaxProgress(user.getLvl());
        if (maxProgress <= progress.getProgress()){
            player.sendMessage("Вы сдали максимальное количество предметов. Дождитесь смены предметов либо повысьте уровень");
            player.playSound(player.getLocation(), failureSellSound, 1.0F, 1.0F);
            return;
        }
        for (int i = 0; i<inventory.getContents().length; i++) {
            final ItemStack itemStack = inventory.getContents()[i];
            if (itemStack == null || itemStack.getType() != material) continue;
            if (count + itemStack.getAmount()+progress.getProgress() <= maxProgress) {
                count += itemStack.getAmount();
                inventory.clear(i);
            }
            else {
                int edge = maxProgress - (progress.getProgress()+count);
                count += edge;
                itemStack.setAmount(itemStack.getAmount() - edge);
                inventory.setItem(i,itemStack);
                break;
            }
        }

        double price = (count/64) * progress.getItem().getPrice(user.getLvl(),mBagsUserManager.getTimeToDynamicReset());
        price = Utils.round(price,2);

        player.playSound(player.getLocation(), successSellSound, 1.0F, 1.0F);
        player.sendMessage("Вы продали "+progress.getItem().getTitle()+ChatColor.WHITE+" x"+count+" за "+ price+ "$");
        Moneybags.getEcon().depositPlayer(player,price);
        progress.setProgress(progress.getProgress()+count);
        update(user);
    }

    @EventHandler
    private void onInventoryClose(InventoryCloseEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        viewing.remove(uuid);
    }
    @EventHandler
    private void onPluginDisable(PluginDisableEvent event) {
        for (final Map.Entry<UUID,Inventory> entry: this.viewing.entrySet()){
            final UUID uuid = entry.getKey();
            final Player p = Bukkit.getPlayer(uuid);
            if (p != null){
                p.closeInventory();
            }
            this.viewing.remove(uuid);
        }
    }

}
