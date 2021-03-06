package me.ebonjaeger.novuspunishment.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.contexts.OnlinePlayer;
import me.ebonjaeger.novuspunishment.*;
import me.ebonjaeger.novuspunishment.action.Warning;
import me.ebonjaeger.novuspunishment.configuration.ActionSettings;
import me.ebonjaeger.novuspunishment.configuration.SettingsManager;
import me.ebonjaeger.novuspunishment.datasource.MySQL;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.time.Instant;

public class WarnCommand extends BaseCommand {

    private BukkitService bukkitService;
    private Messenger messenger;
    private MySQL dataSource;
    private SettingsManager settings;
    private StateManager stateManager;

    @Inject WarnCommand(BukkitService bukkitService, Messenger messenger, MySQL dataSource, SettingsManager settings, StateManager stateManager) {
        this.bukkitService = bukkitService;
        this.messenger = messenger;
        this.dataSource = dataSource;
        this.settings = settings;
        this.stateManager = stateManager;
    }

    @CommandAlias("warn")
    @CommandPermission("newpunish.command.warn")
    @CommandCompletion("@players")
    public void onCommand(CommandSender sender, OnlinePlayer player, String... reason) {
        Player target = player.getPlayer();
        String _reason = String.join(", ", reason);

        if (sender.getName().equals(target.getName())) {
            messenger.sendMessage(sender, Message.ACTION_AGAINST_SELF);
            return;
        }

        if (target.hasPermission("newpunish.bypass.warn")) {
            messenger.sendMessage(sender, Message.WARN_EXEMPT, target.getName());
            return;
        }

        String staff = "console";
        if (sender instanceof Player) {
            staff = ((Player) sender).getUniqueId().toString();
        }

        Instant timestamp = Instant.now();
        Warning warning = new Warning(target.getUniqueId(), staff, timestamp, _reason);

        bukkitService.runTaskAsync(() -> dataSource.saveWarning(warning));

        stateManager.incrementWarnings(target.getUniqueId());
        int sessionCount = stateManager.getWarnings(target.getUniqueId());
        int warnLimit = settings.getProperty(ActionSettings.WARNS_UNTIL_KICK);

        if (warnLimit > 0 && sessionCount % warnLimit == 0) {
            // TODO: Make it clear they were warned too many times
            target.kickPlayer(Utils.formatKickMessage(warning.getReason()));
        } else {
            messenger.sendMessage(target, Message.WARN_PLAYER, warning.getReason());
        }

        messenger.broadcastMessageExcept(Message.WARN_NOTIFICATION, target, "newpunish.notify.warn",
            target.getName(), warning.getReason()
        );
    }
}
