package de.nikos410.discordBot.modules;

import de.nikos410.discordBot.DiscordBot;
import de.nikos410.discordBot.util.general.Util;
import de.nikos410.discordBot.util.modular.annotations.CommandModule;
import de.nikos410.discordBot.util.modular.CommandPermissions;
import de.nikos410.discordBot.util.modular.annotations.CommandSubscriber;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@CommandModule(moduleName = "Nutzergruppen", commandOnly = true)
public class UserGroups {
    private final static Path USERGROUPS_PATH = Paths.get("data/usergroups.json");
    private JSONObject usergroupsJSON;

    private final DiscordBot bot;

    private Logger log = LoggerFactory.getLogger(UserGroups.class);


    public UserGroups(final DiscordBot bot) {
        this.bot = bot;

        final String jsonContent = Util.readFile(USERGROUPS_PATH);
        usergroupsJSON = new JSONObject(jsonContent);
    }

    @CommandSubscriber(command = "createGroup", help = "Neue Gruppe erstellen", pmAllowed = false, permissionLevel = CommandPermissions.MODERATOR)
    public void command_createGroup(final IMessage message, final String groupName) {

        if (usergroupsJSON.has(groupName)) {
            Util.sendMessage(message.getChannel(), ":x: Gruppe existiert bereits!");
            return;
        }

        final IRole role = message.getGuild().createRole();
        role.changePermissions(EnumSet.noneOf(Permissions.class));
        role.changeName(groupName);
        role.changeMentionable(true);

        usergroupsJSON.put(groupName, role.getLongID());
        saveJSON();

        Util.sendMessage(message.getChannel(), String.format(":white_check_mark: Gruppe `%s` erstellt.", groupName));
        log.info(String.format("%s created new group %s.", Util.makeUserString(message.getAuthor(), message.getGuild()), groupName));
    }

    @CommandSubscriber(command = "removeGroup", help = "Gruppe entfernen", pmAllowed = false, permissionLevel = CommandPermissions.MODERATOR)
    public void command_removeGroup(final IMessage message, final String groupName) {

        if (!usergroupsJSON.has(groupName)) {
            Util.sendMessage(message.getChannel(), String.format(":x: Gruppe `%s` nicht gefunden!", groupName));
            return;
        }

        final IRole role = message.getGuild().getRoleByID(usergroupsJSON.getLong(groupName));
        role.delete();

        usergroupsJSON.remove(groupName);
        saveJSON();

        Util.sendMessage(message.getChannel(), String.format(":white_check_mark: Gruppe `%s` entfernt.", groupName));
        log.info(String.format("%s deleted group %s.", Util.makeUserString(message.getAuthor(), message.getGuild()), groupName));
    }

    @CommandSubscriber(command = "group", help = "Sich selbst eine Rolle zuweisen / wieder entfernen", pmAllowed = false)
    public void command_Group(final IMessage message, final String groupName) {
        final IUser user = message.getAuthor();
        final IGuild guild = message.getGuild();

        if (usergroupsJSON.has(groupName)) {
            // Gruppe existiert bereits

            final long roleID = usergroupsJSON.getLong(groupName);
            final IRole role = message.getGuild().getRoleByID(roleID);

            if (Util.hasRole(user, role, guild)) {
                user.removeRole(role);
                Util.sendMessage(message.getChannel(), String.format(":white_check_mark: Du wurdest aus der Gruppe `%s` entfernt.", groupName));
                log.info(String.format("%s left group %s.", Util.makeUserString(message.getAuthor(), message.getGuild()), groupName));
            }
            else {
                user.addRole(role);
                Util.sendMessage(message.getChannel(), String.format(":white_check_mark: Du wurdest zur Gruppe `%s` hinzugefügt.", groupName));
                log.info(String.format("%s joined group %s.", Util.makeUserString(message.getAuthor(), message.getGuild()), groupName));
            }
        }
        else {
            Util.sendMessage(message.getChannel(), String.format(":x: Gruppe `%s` nicht gefunden!", groupName));
        }
    }

    @CommandSubscriber(command = "groups", help = "Alle Rollen auflisten")
    public void command_groups(final IMessage message) {
        final StringBuilder stringBuilder = new StringBuilder();

        final List<String> keyList = new LinkedList<>();
        keyList.addAll(usergroupsJSON.keySet());
        Collections.sort(keyList);

        for (String key : keyList) {
            if (stringBuilder.length() != 0) {
                stringBuilder.append('\n');
            }
            stringBuilder.append(key);
        }
        if (stringBuilder.length() == 0) {
            stringBuilder.append("_Keine_");
        }

        final EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.appendField("Verfügbare Gruppen:", stringBuilder.toString(), false);

        embedBuilder.withFooterText(String.format("Weise dir mit '%sgroup <Gruppe> selbst eine dieser Gruppen zu'", bot.configJSON.getString("prefix")));

        Util.sendEmbed(message.getChannel(), embedBuilder.build());
    }

    private void saveJSON() {
        log.debug("Saving UserGroups file.");

        final String jsonOutput = usergroupsJSON.toString(4);
        Util.writeToFile(USERGROUPS_PATH, jsonOutput);
    }
}
