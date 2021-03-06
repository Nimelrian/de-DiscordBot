package de.nikos410.discordBot.util.general;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.util.DiscordException;

/**
 * Authorizes the Bot and returns Instance
 */
public class Authorization {
    private static Logger log = LoggerFactory.getLogger(Authorization.class);

    public static IDiscordClient createClient(final String token, final boolean login) {
        ClientBuilder clientBuilder = new ClientBuilder(); // Creates the ClientBuilder instance
        clientBuilder.withToken(token); // Adds the login info to the builder
        try {
            if (login) {
                return clientBuilder.login(); // Creates the client instance and logs the client in
            } else {
                return clientBuilder.build(); // Creates the client instance but it doesn't log the client in yet, you would have to call client.login() yourself
            }
        } catch (DiscordException e) { // This is thrown if there was a problem building the client
            log.error("Could not auhorize the bot. Please make sure your token is correct.", e);
            throw e;
        }
    }
}