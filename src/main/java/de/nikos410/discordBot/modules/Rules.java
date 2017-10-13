package de.nikos410.discordBot.modules;


import de.nikos410.discordBot.DiscordBot;
import de.nikos410.discordBot.util.general.Util;
import de.nikos410.discordBot.util.modular.CommandModule;
import de.nikos410.discordBot.util.modular.CommandPermissions;
import de.nikos410.discordBot.util.modular.CommandSubscriber;
import org.json.JSONObject;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

@CommandModule(moduleName = "Regeln", commandOnly = false)
public class Rules {
    private final static Path RULES_PATH = Paths.get("data/rules.json");

    private final DiscordBot bot;

    private JSONObject jsonWelcome;
    private String welcomeMessage;
    private String rulesDE;
    private String rulesEN;
    private String welcomeFooter;
    private boolean isEnabled;

    public Rules (final DiscordBot bot) {
        this.bot = bot;

        // Welcome Nachricht auslesen
        final String welcomeFileContent = Util.readFile(RULES_PATH);
        this.jsonWelcome = new JSONObject(welcomeFileContent);

        this.welcomeMessage = jsonWelcome.getString("welcome");
        this.rulesDE = jsonWelcome.getString("rulesDE");
        this.rulesEN = jsonWelcome.getString("rulesEN");
        this.welcomeFooter = jsonWelcome.getString("footer");
        this.isEnabled = jsonWelcome.getBoolean("on");
    }

    @EventSubscriber
    public void onUserJoin(UserJoinEvent event) {
        if (this.isEnabled) {
            Util.sendPM(event.getUser(), welcomeMessage + "\n\n" + rulesDE + "\n\n\n" + String.format(welcomeFooter, this.bot.prefix));
        }
    }

    @CommandSubscriber(command = "regeln", help = "Die Regeln dieses Servers", pmAllowed = true, permissionLevel = CommandPermissions.EVERYONE)
    public void command_Regeln(final IMessage message) {
        Util.sendPM(message.getAuthor(), this.rulesDE);

        if (!message.getChannel().isPrivate()) {
            Util.sendMessage(message.getChannel(), ":mailbox_with_mail:");
        }
    }

    @CommandSubscriber(command = "rules", help = "The rules of this server", pmAllowed = true, permissionLevel = CommandPermissions.EVERYONE)
    public void command_Rules(final IMessage message) {
        Util.sendPM(message.getAuthor(), this.rulesEN);

        if (!message.getChannel().isPrivate()) {
            Util.sendMessage(message.getChannel(), ":mailbox_with_mail:");
        }
    }

    @CommandSubscriber(command = "welcomeset_test", help = "Begrüßungsnachricht testen", pmAllowed = true, permissionLevel = CommandPermissions.EVERYONE)
    public void command_Welcomeset_Test(final IMessage message) {
        Util.sendPM(message.getAuthor(), welcomeMessage + "\n\n" + rulesDE + "\n\n\n" + String.format(welcomeFooter, this.bot.prefix));
    }

    @CommandSubscriber(command = "welcomeset_enable", help = "Begrüßungsnachricht aktivieren", pmAllowed = true, permissionLevel = CommandPermissions.ADMIN)
    public void command_Welcomeset_Enable(final IMessage message) {
        this.isEnabled = true;
        if (jsonWelcome.has("on")) {
            jsonWelcome.remove("on");
        }
        jsonWelcome.put("on", true);
        this.saveJSON();

        Util.sendMessage(message.getChannel(), ":white_check_mark: Aktiviert!");
    }

    @CommandSubscriber(command = "welcomeset_disable", help = "Begrüßungsnachricht deaktivieren", pmAllowed = true, permissionLevel = CommandPermissions.ADMIN)
    public void command_Welcomeset_Disable(final IMessage message) {
        this.isEnabled = false;
        if (jsonWelcome.has("on")) {
            jsonWelcome.remove("on");
        }
        jsonWelcome.put("on", false);
        this.saveJSON();

        Util.sendMessage(message.getChannel(), ":x: Deaktiviert!");
    }

    @CommandSubscriber(command = "welcomeset_welcome", help = "Begrüßungsnachricht ändern", pmAllowed = true, permissionLevel = CommandPermissions.ADMIN)
    public void command_Welcomeset_Welcome(final IMessage message) {
        this.welcomeMessage = Util.getContext(message.getContent());
        if (jsonWelcome.has("welcome")) {
            jsonWelcome.remove("welcome");
        }
        jsonWelcome.put("welcome", this.welcomeMessage);
        this.saveJSON();

        Util.sendMessage(message.getChannel(), ":white_check_mark: Begrüßungs-Nachricht geändert");
        Util.sendMessage(message.getChannel(), this.welcomeMessage);
    }

    @CommandSubscriber(command = "welcomeset_regeln", help = "Regeln (deutsch) ändern", pmAllowed = true, permissionLevel = CommandPermissions.ADMIN)
    public void command_Welcomeset_Regeln(final IMessage message) {
        this.rulesDE = Util.getContext(message.getContent());
        if (jsonWelcome.has("rulesDE")) {
            jsonWelcome.remove("rulesDE");
        }
        jsonWelcome.put("rulesDE", this.rulesDE);
        this.saveJSON();

        Util.sendMessage(message.getChannel(), ":white_check_mark: Regeln geändert:");
        Util.sendMessage(message.getChannel(), this.rulesDE);
    }

    @CommandSubscriber(command = "welcomeset_rules", help = "Regeln (englisch) ändern", pmAllowed = true, permissionLevel = CommandPermissions.ADMIN)
    public void command_Welcomeset_Rules(final IMessage message) {
        this.rulesEN = Util.getContext(message.getContent());
        if (jsonWelcome.has("rulesEN")) {
            jsonWelcome.remove("rulesEN");
        }
        jsonWelcome.put("rulesEN", this.rulesEN);
        this.saveJSON();

        Util.sendMessage(message.getChannel(), ":white_check_mark: Regeln geändert:");
        Util.sendMessage(message.getChannel(), this.rulesEN);
    }

    @CommandSubscriber(command = "welcomeset_footer", help = "Footer der Begüßungsnachricht ändern. `%s` für Befehls-Prefix",
            pmAllowed = true, permissionLevel = CommandPermissions.ADMIN)
    public void command_Welcomeset_Footer(final IMessage message) {
        this.welcomeFooter = Util.getContext(message.getContent());
        if (jsonWelcome.has("footer")) {
            jsonWelcome.remove("footer");
        }
        jsonWelcome.put("footer", this.welcomeFooter);
        this.saveJSON();

        Util.sendMessage(message.getChannel(), ":white_check_mark: Begrüßungs-Footer geändert");
        Util.sendMessage(message.getChannel(), String.format(this.welcomeFooter, this.bot.prefix));
    }

    private void saveJSON() {
        final String jsonOutput = jsonWelcome.toString(4);
        Util.writeToFile(RULES_PATH, jsonOutput);

        jsonWelcome = new JSONObject(jsonOutput);
    }
}