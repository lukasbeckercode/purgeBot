import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGES;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_VOICE_STATES;

public class TestBot {
    public static void main(String[] args) {

        try {
            new TestBot();
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicMangers;
    private final JDA jda;

    public TestBot() throws LoginException {
       // jda = JDABuilder.createDefault("NzkwMjEyODQxNDQ5MDYyNDEw.X99VDg.bfpnrkjQJy8U5PJ3ybN9bieuC4I").build();

        jda = JDABuilder.create("NzkwMjEyODQxNDQ5MDYyNDEw.X99VDg.bfpnrkjQJy8U5PJ3ybN9bieuC4I", GUILD_MESSAGES,GUILD_VOICE_STATES)
                .addEventListeners(new TestBot()).build();

        jda.getPresence().setActivity(Activity.watching("Lukas fail at programming"));



        musicMangers = new HashMap<>();
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        System.out.println("Bot is online");

        stopBot();
    }





    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild){
        long guildID = Long.parseLong(guild.getId());

        GuildMusicManager musicManger = musicMangers.get(guildID);
        if(musicManger == null){
            musicManger = new GuildMusicManager(playerManager);
            musicMangers.put(guildID,musicManger);
        }

        guild.getAudioManager().setSendingHandler(musicManger.getSendHandler());

        return musicManger;
    }
    public void onGuildMessageReceived(GuildMessageReceivedEvent event){
        String message = event.getMessage().getContentDisplay();
        if (message.equals("-purge")) {
            loadAndPlay(event.getChannel());
        }
    }

    private void loadAndPlay(final TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        playerManager.loadItemOrdered(musicManager, "https://www.youtube.com/watch?v=Qmm-Ivuphzw", new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                channel.sendMessage("Adding to queue " + track.getInfo().title).queue();

                play(channel.getGuild(), musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                channel.sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

                play(channel.getGuild(), musicManager, firstTrack);
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Nothing found by " + "https://www.youtube.com/watch?v=Qmm-Ivuphzw").queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });
    }

    private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
            connectToFirstVoiceChannel(guild.getAudioManager());

            musicManager.scheduler.queue(track);
    }

    private void connectToFirstVoiceChannel(AudioManager audioManager) {
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
                audioManager.openAudioConnection(voiceChannel);
                break;
            }
        }
    }




    void stopBot() {
        new Thread(() -> {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                while ((line = reader.readLine()) != null) {
                    if (line.equalsIgnoreCase("exit")) {
                        if (jda != null) {
                            jda.getPresence().setStatus(OnlineStatus.OFFLINE);
                            jda.shutdown();
                            System.out.println("bot offline");
                            reader.close();
                            System.exit(0);
                        }
                    }
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}

