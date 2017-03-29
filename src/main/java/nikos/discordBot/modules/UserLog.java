package nikos.discordBot.modules;


import nikos.discordBot.util.Util;
import org.json.JSONObject;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserBanEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserLeaveEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UserLog {
    private final static Path USERLOG_PATH = Paths.get("data/userLog.json");
    private final static Path CONFIG_PATH = Paths.get("config/config.json");

    private final String prefix;
    private final String ownerID;

    private JSONObject jsonUserLog;
    private IChannel userLogChannel;
    private boolean on;

    static IDiscordClient client;

    public UserLog(final IDiscordClient dClient) {
        client = dClient;

        // Prefix und Owner ID aus Config-Datei auslesen
        final String configFileContent = Util.readFile(CONFIG_PATH);
        if (configFileContent == null) {
            throw new RuntimeException("[ERROR] Could not read Config File!");
        }
        final JSONObject jsonConfig = new JSONObject(configFileContent);
        this.prefix = jsonConfig.getString("prefix");
        this.ownerID = jsonConfig.getString("owner");
    }

    @EventSubscriber
    public void onStartup(ReadyEvent event) {
        // UserLog Kanal auslesen
        final String userLogFileContent = Util.readFile(USERLOG_PATH);
        if (userLogFileContent == null) {
            throw new RuntimeException("[ERROR] Could not read userLog File!");
        }
        jsonUserLog = new JSONObject(userLogFileContent);

        this.on = jsonUserLog.getBoolean("on");
        final String channelID = jsonUserLog.getString("channel");
        this.userLogChannel = client.getChannelByID(channelID);
        if (userLogChannel == null) {
            System.err.print("[ERR] Invalid UserLog Channel!");
        }
    }

    @EventSubscriber
    public void onUserJoin(UserJoinEvent event) {
        if (this.on) {
            this.userJoin(event.getUser());
        }
    }

    @EventSubscriber
    public void onUnserLeave(UserLeaveEvent event) {
        if (this.on) {
            this.userLeave(event.getUser());
        }
    }

    @EventSubscriber
    public void onUserBan(UserBanEvent event) {
        if (this.on) {
            this.userBan(event.getUser());
        }
    }

    @EventSubscriber
    public void onMessageRecieved(MessageReceivedEvent event) {
        final IMessage message = event.getMessage();
        if (!message.getAuthor().getID().equals(ownerID)) {
            return;
        }

        final String messageContent = message.getContent();

        if (messageContent.toLowerCase().equals(prefix + "userlog")) {
            Util.sendMessage(message.getChannel(), "Kanal: <#" + userLogChannel.getID() + ">");
        }
        else if (messageContent.toLowerCase().startsWith(prefix + "userlog channel")) {
            final String channelID = Util.getContext(Util.getContext(messageContent));
            if (jsonUserLog.has("channel")) {
                jsonUserLog.remove("channel");
            }
            jsonUserLog.put("channel", channelID);
            saveUserLogJSON();

            this.userLogChannel = client.getChannelByID(channelID);
            if (userLogChannel == null) {
                System.err.print("[ERR] Invalid UserLog Channel!");
                Util.sendMessage(message.getChannel(), ":x: Kanal mit der ID `" + channelID + "` nicht gefunden!");
            }
            else {
                Util.sendMessage(message.getChannel(), ":white_check_mark: Neuer Kanal: " +
                        "<#" + userLogChannel.getID() + ">");
            }
        }
        else if (messageContent.toLowerCase().startsWith(prefix + "userlog enable")) {
            this.on = true;
            if (jsonUserLog.has("on")) {
                jsonUserLog.remove("on");
            }
            jsonUserLog.put("on", true);
            saveUserLogJSON();

            Util.sendMessage(message.getChannel(), ":white_check_mark: Aktiviert!");
        }
        else if (messageContent.toLowerCase().startsWith(prefix + "userlog disable")) {
            this.on = false;
            if (jsonUserLog.has("on")) {
                jsonUserLog.remove("on");
            }
            jsonUserLog.put("on", false);
            saveUserLogJSON();

            Util.sendMessage(message.getChannel(), ":x: Deaktiviert!");
        }
        else if (messageContent.equals(prefix + "userlog test")) {
            IUser user = message.getAuthor();
            userJoin(user);
            userLeave(user);
            userBan(user);
        }
    }

    private void userJoin(final IUser user) {
        final EmbedBuilder embedBuilder = new EmbedBuilder();
        final DateTimeFormatter serverJoinTimeStampFormatter = DateTimeFormatter.ofPattern("dd.MM. | HH:mm");
        final DateTimeFormatter discordJoinTimestampFormatter = DateTimeFormatter.ofPattern("dd.MM.YYYY | HH:mm");

        embedBuilder.withThumbnail(user.getAvatarURL());
        embedBuilder.appendField(":white_check_mark: Nutzer ist dem Server beigetreten!",
                        "**Name:** " + user.getName() + '#' + user.getDiscriminator() + '\n' +
                        "**ID:** " + user.getID() + '\n' +
                        "**Discord beigetreten:** " + user.getCreationDate().format(discordJoinTimestampFormatter),
                false);
        embedBuilder.withFooterText(LocalDateTime.now().format(serverJoinTimeStampFormatter));

        final EmbedObject embedObject = embedBuilder.build();

        Util.sendEmbed(userLogChannel, embedObject);
    }

    private void userLeave(final IUser user) {
        final EmbedBuilder embedBuilder = new EmbedBuilder();
        final DateTimeFormatter timeStampFormatter = DateTimeFormatter.ofPattern("dd.MM. | HH:mm");

        embedBuilder.withThumbnail(user.getAvatarURL());
        embedBuilder.appendField(":x: Nutzer hat den Server verlassen!",
                "**Name:** " + user.getName() + '#' + user.getDiscriminator() + '\n' +
                "**ID:** " + user.getID(),
                false);
        embedBuilder.withFooterText(LocalDateTime.now().format(timeStampFormatter));

        final EmbedObject embedObject = embedBuilder.build();

        Util.sendEmbed(userLogChannel, embedObject);
    }

    private void userBan(final IUser user) {
        final EmbedBuilder embedBuilder = new EmbedBuilder();
        final DateTimeFormatter timeStampFormatter = DateTimeFormatter.ofPattern("dd.MM. | HH:mm");

        embedBuilder.withThumbnail(user.getAvatarURL());
        embedBuilder.appendField(":hammer: Nutzer gebannt!",
                "**Name:** " + user.getName() + '#' + user.getDiscriminator() + '\n' +
                "**ID:** " + user.getID(),
                false);
        embedBuilder.withFooterText(LocalDateTime.now().format(timeStampFormatter));

        final EmbedObject embedObject = embedBuilder.build();

        Util.sendEmbed(userLogChannel, embedObject);
    }

    private void saveUserLogJSON() {
        final String jsonOutput = jsonUserLog.toString(4);
        Util.writeToFile(USERLOG_PATH, jsonOutput);

        jsonUserLog = new JSONObject(jsonOutput);
    }
}