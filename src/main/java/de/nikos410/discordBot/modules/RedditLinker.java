package de.nikos410.discordBot.modules;

import de.nikos410.discordBot.util.general.Util;
import de.nikos410.discordBot.util.modular.annotations.CommandModule;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CommandModule(moduleName = "Reddit-Linker", commandOnly = false)
public class RedditLinker {

    @EventSubscriber
    public void onMessageReceived(final MessageReceivedEvent event) {
        final IMessage message = event.getMessage();
        final String messageContent = message.getContent();

        Pattern pattern = Pattern.compile("^/?r/(\\w+)$");
        Matcher matcher = pattern.matcher(messageContent);


        if (matcher.matches()) {
            Util.sendMessage(message.getChannel(), "https://www.reddit.com/r/" + matcher.group(1));
        }
    }
}