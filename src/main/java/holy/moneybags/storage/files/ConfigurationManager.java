package holy.moneybags.storage.files;

import holy.moneybags.Moneybags;

import java.io.*;
import java.util.HashMap;

public class  ConfigurationManager {

    private HashMap<String, Configuration> Configs;
    public ConfigurationManager() {
        this.Configs = new HashMap();
    }


    public Configuration getConfig(String name) {
        if (this.Configs.containsKey(name)) {
            return this.Configs.get(name);
        }
        Configuration config = new Configuration(Moneybags.getInstance(), name);
        this.Configs.put(name, config);
        return config;
    }

    public void reloadConfigs() {
        for  (Configuration c: Configs.values()) c.reload();
    }


    public void createFile(String name) {
        InputStream istr = null;
        OutputStream ostr = null;
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            istr = classLoader.getResourceAsStream(name.replace('\\', '/'));


            if (istr == null) {
                Moneybags.getInstance().getLogger().warning("FAILED TO CREATE PLUGIN CONFIG! [No source file in the plugin jar]");
            } else {
                Moneybags.getInstance().getDataFolder().mkdirs();
                ostr = new FileOutputStream(new File(Moneybags.getInstance().getDataFolder(), name));
                byte[] buffer = new byte[99];
                int length = 0;
                length = istr.read(buffer);
                while (length > 0) {
                    ostr.write(buffer, 0, length);
                    length = istr.read(buffer);
                }
            }
            return;
        } catch (IOException ex) {
            Moneybags.getInstance().getLogger().warning("FAILED TO CREATE PLUGIN CONFIG! [IOException 1]");
            ex.printStackTrace();
        } finally {
            try {
                if (istr != null) {
                    istr.close();
                }
            } catch (IOException ex) {
                Moneybags.getInstance().getLogger().warning("FAILED TO CREATE PLUGIN CONFIG! [IOException 2]");
                ex.printStackTrace();
            }
            try {
                if (ostr != null) {
                    ostr.close();
                }
            } catch (IOException ex) {
                Moneybags.getInstance().getLogger().warning("FAILED TO CREATE PLUGIN CONFIG! [IOException 3]");
                ex.printStackTrace();
            }
        }
    }
}
