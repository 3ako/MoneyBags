package holy.moneybags;

import holy.moneybags.commands.CommandMoneybags;
import holy.moneybags.gui.MBagsMenu;
import holy.moneybags.gui.mbags.MBagsUserManager;
import holy.moneybags.gui.mbags.items.DynamicItemsManager;
import holy.moneybags.gui.mbags.items.StaticItemsManager;
import holy.moneybags.storage.StorageManager;
import holy.moneybags.storage.files.ConfigurationManager;
import holy.moneybags.storage.mysql.MySqlStorage;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Moneybags extends JavaPlugin{
    @Getter
    private static Moneybags instance;
    @Getter
    private static final ConfigurationManager configurationManager = new ConfigurationManager();
    @Getter
    private static Economy econ = null;
    @Getter
    private StorageManager storageManager;
    @Getter
    private StaticItemsManager staticItemsManager;
    @Getter
    private DynamicItemsManager dynamicItemsManager;
    @Getter
    private MBagsUserManager mBagsUserManager;
    @Getter
    private static MBagsMenu mBagsMenu;
    @Override
    public void onEnable() {
        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.instance = this;
        loadConfigs();

        storageManager      = new StorageManager(new MySqlStorage());
        staticItemsManager  = new StaticItemsManager(configurationManager.getConfig("items.yml").getFile());
        dynamicItemsManager = new DynamicItemsManager(configurationManager.getConfig("items.yml").getFile());
        mBagsUserManager    = new MBagsUserManager(this,storageManager.getStorage(),staticItemsManager);
        mBagsMenu           = new MBagsMenu(this,mBagsUserManager);

        getServer().getPluginCommand("moneybags").setExecutor(new CommandMoneybags());
    }

    private void loadConfigs(){
        try {
            if (!(new File(getDataFolder(), "config.yml")).exists()) {
                getConfigurationManager().createFile("config.yml");
            }
            if (!(new File(getDataFolder(), "items.yml")).exists()) {
                getConfigurationManager().createFile("items.yml");
            }
            if (!(new File(getDataFolder(), "lang.yml")).exists()) {
                getConfigurationManager().createFile("lang.yml");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
}
