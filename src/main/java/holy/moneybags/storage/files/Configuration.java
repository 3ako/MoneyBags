package holy.moneybags.storage.files;

import holy.moneybags.Moneybags;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Configuration {

    private JavaPlugin p;
    private FileConfiguration config;
    private File file; private String name;


    public Configuration(JavaPlugin plugin, String absolutePath) {
        this.config = null;
        this.file = null;

        this.p = plugin;
        this.name = absolutePath;
    }
    public void reload() {
        if (this.file == null) {
            this.file = new File(this.p.getDataFolder(), this.name);
        }
        this.config = YamlConfiguration.loadConfiguration(this.file);
        if (!this.file.exists()) {
            this.file.getParentFile().mkdirs();
            Moneybags.getConfigurationManager().createFile(this.name);
        }
        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new File(this.name));
        this.config.setDefaults(defConfig);
    }

    public FileConfiguration getFile() {
        if (this.config == null) {
            reload();
        }
        return this.config;
    }

    public void saveDefaultConfig() {
        if (this.file == null) {
            this.file = new File(this.name);
        }
        if (!this.file.exists()) {
            this.p.saveResource(this.name, false);
        }
    }

    public void save() {
        if (this.config == null || this.file == null) {
            return;
        }
        try {
            getFile().save(this.file);
        } catch (IOException ex) {
            this.p.getLogger().log(Level.SEVERE, "Could not save config to " + this.file, ex);
        }
    }

    public String c(String name) {
        String caption = getFile().getString(name);
        if (caption == null) {
            this.p.getLogger().warning("No such language caption found: " + name);
            caption = "&c[No language caption found]";
        }
        return ChatColor.translateAlternateColorCodes('&', caption);
    }


    public String cp(String name) {
        String caption = getFile().getString(name);
        if (caption == null) {
            this.p.getLogger().warning("No such language caption found: " + name);
            caption = "&c[No language caption found]";
        }
        return ChatColor.translateAlternateColorCodes('&', String.valueOf(getFile().getString("prefix")) + caption);
    }


    public List<String> cl(String name) {
        List<String> captionlist = new ArrayList<String>();
        for (String s : getFile().getStringList(name)) {
            captionlist.add(ChatColor.translateAlternateColorCodes('&', s));
        }
        if (getFile().getStringList(name) == null) {
            this.p.getLogger().warning("No such language caption found: " + name);
            captionlist.add(ChatColor.translateAlternateColorCodes('&', "&c[No language caption found]"));
        }
        return captionlist;
    }
}