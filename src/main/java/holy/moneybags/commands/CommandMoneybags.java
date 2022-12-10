package holy.moneybags.commands;

import holy.moneybags.Moneybags;
import holy.moneybags.mbags.MBagsUser;
import holy.moneybags.storage.files.Configuration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandMoneybags implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final Configuration lang = Moneybags.getConfigurationManager().getConfig("lang.yml");
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда для игроков");
            return true;
        }
        final Player player = (Player) sender;

        final MBagsUser user = Moneybags.getInstance().getMBagsUserManager().getUser(player);
        if (user == null) {
            player.sendMessage(lang.c("error.unableCash"));
            return true;
        }

        Moneybags.getMBagsMenu().open(user);
        return true;
    }
}
