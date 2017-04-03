package nikos.discordBot.modules;

import nikos.discordBot.util.Util;
import org.json.JSONObject;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.EmbedBuilder;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Rules {
    private final static Path RULES_PATH = Paths.get("data/rules.json");
    private final static Path CONFIG_PATH = Paths.get("config/config.json");

    private static final String MODULE_NAME = "Regeln";
    private static final char SEPARATOR = '⠀';
    private static final String COMMANDS =
                    "`regeln         " + SEPARATOR + "`  Schickt die Regeln dieses Servers" + '\n' +
                    "`rules          " + SEPARATOR + "`  Sends Rules of this server";

    private final String prefix;
    private final String ownerID;

    private JSONObject jsonWelcome;
    private String welcomeMessage;
    private String rulesDE;
    private String rulesEN;
    private boolean isEnabled;

    public Rules(IDiscordClient dClient) {

        // Prefix und Owner ID aus Config-Datei auslesen
        final String configFileContent = Util.readFile(CONFIG_PATH);
        final JSONObject jsonConfig = new JSONObject(configFileContent);

        this.prefix = jsonConfig.getString("prefix");
        this.ownerID = jsonConfig.getString("owner");

        // Welcome Nachricht auslesen
        final String welcomeFileContent = Util.readFile(RULES_PATH);
        this.jsonWelcome = new JSONObject(welcomeFileContent);

        this.welcomeMessage = jsonWelcome.getString("welcome");
        this.rulesDE = jsonWelcome.getString("rulesDE");
        this.rulesEN = jsonWelcome.getString("rulesEN");
        this.isEnabled = jsonWelcome.getBoolean("on");
    }

    @EventSubscriber
    public void onUserJoin(UserJoinEvent event) {
        if (this.isEnabled) {
            final IUser user = event.getUser();

            Util.sendPM(user, welcomeMessage);
            Util.sendPM(user, rulesDE);
            Util.sendPM(user, "**If you don't speak german, use the command `" + prefix + "rules`.**");
        }
    }

    @EventSubscriber
    public void onMessageRecieved(MessageReceivedEvent event) {

        final IMessage message = event.getMessage();
        final String messageContent = message.getContent();
        final String messageContentLowerCase = messageContent.toLowerCase();

        if (messageContentLowerCase.startsWith(prefix + "help")) {
            this.command_Help(message);
        }
        else if (messageContentLowerCase.equals(prefix + "regeln")) {
            this.command_Regeln(message);
        }
        else if (messageContentLowerCase.equals(prefix + "rules")) {
            this.command_Rules(message);
        }

        // Folgende Befehle nur Owner
        if (!message.getAuthor().getID().equals(ownerID)) {
            return;
        }

        if (messageContent.equalsIgnoreCase(prefix + "welcomeset")) {
            this.command_Welcomeset(message);
        }
        else if (messageContentLowerCase.startsWith(prefix + "welcomeset welcome ")) {
            this.command_Welcomeset_Welcome(message);
        }
        else if (messageContentLowerCase.startsWith(prefix + "welcomeset regeln ")) {
            this.command_Welcomeset_Regeln(message);
        }
        else if (messageContentLowerCase.startsWith(prefix + "welcomeset rules ")) {
            this.command_Welcomeset_Rules(message);
        }
        else if (messageContentLowerCase.equals(prefix + "welcomeset enable")) {
            this.command_Welcomeset_Enable(message);
        }
        else if (messageContentLowerCase.equals(prefix + "welcomeset disable")) {
            this.command_Welcomeset_Disable(message);
        }
    }

    /**********
     * COMMANDS
     **********/

    private void command_Help(final IMessage message) {
        final EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.withColor(new Color(114, 137, 218));
        embedBuilder.appendField(MODULE_NAME, COMMANDS, false);

        final EmbedObject embedObject = embedBuilder.build();

        Util.sendEmbed(message.getChannel(), embedObject);
    }

    private void command_Regeln(final IMessage message) {
        Util.sendPM(message.getAuthor(), this.rulesDE);

        if (!message.getChannel().isPrivate()) {
            Util.sendMessage(message.getChannel(), ":mailbox_with_mail:");
        }
    }

    private void command_Rules(final IMessage message) {
        Util.sendPM(message.getAuthor(), this.rulesEN);

        if (!message.getChannel().isPrivate()) {
            Util.sendMessage(message.getChannel(), ":mailbox_with_mail:");
        }
    }

    private void command_Welcomeset(final IMessage message) {
        // Per PM antworten
        Util.sendPM(message.getAuthor(), this.welcomeMessage);
        Util.sendPM(message.getAuthor(), this.rulesDE);
        Util.sendPM(message.getAuthor(), "**If you don't speak german, use the command `" +
                prefix + "rules`.**");

    }

    private void command_Welcomeset_Welcome(final IMessage message) {
        this.welcomeMessage = Util.getContext(Util.getContext(message.getContent()));
        if (jsonWelcome.has("welcome")) {
            jsonWelcome.remove("welcome");
        }
        jsonWelcome.put("welcome", this.welcomeMessage);
        this.saveJSON();

        Util.sendMessage(message.getChannel(), ":white_check_mark: Begrüßungs-Nachricht geändert");
        Util.sendMessage(message.getChannel(), welcomeMessage);
    }

    private void command_Welcomeset_Rules(final IMessage message) {
        this.rulesEN = Util.getContext(Util.getContext(message.getContent()));
        if (jsonWelcome.has("rulesEN")) {
            jsonWelcome.remove("rulesEN");
        }
        jsonWelcome.put("rulesEN", this.rulesEN);
        this.saveJSON();

        Util.sendMessage(message.getChannel(), ":white_check_mark: Regeln geändert:");
        Util.sendMessage(message.getChannel(), rulesDE);
    }

    private void command_Welcomeset_Regeln(final IMessage message) {
        this.rulesDE = Util.getContext(Util.getContext(message.getContent()));
        if (jsonWelcome.has("rulesDE")) {
            jsonWelcome.remove("rulesDE");
        }
        jsonWelcome.put("rulesDE", this.rulesDE);
        this.saveJSON();

        Util.sendMessage(message.getChannel(), ":white_check_mark: Regeln geändert:");
        Util.sendMessage(message.getChannel(), rulesDE);
    }

    private void command_Welcomeset_Enable(final IMessage message) {
        this.isEnabled = true;
        if (jsonWelcome.has("on")) {
            jsonWelcome.remove("on");
        }
        jsonWelcome.put("on", true);
        this.saveJSON();

        Util.sendMessage(message.getChannel(), ":white_check_mark: Aktiviert!");
    }

    private void command_Welcomeset_Disable(final IMessage message) {
        this.isEnabled = false;
        if (jsonWelcome.has("on")) {
            jsonWelcome.remove("on");
        }
        jsonWelcome.put("on", false);
        this.saveJSON();

        Util.sendMessage(message.getChannel(), ":x: Deaktiviert!");
    }

    private void saveJSON() {
        final String jsonOutput = jsonWelcome.toString(4);
        Util.writeToFile(RULES_PATH, jsonOutput);

        jsonWelcome = new JSONObject(jsonOutput);
    }
}
