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
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


public class TestBot extends ListenerAdapter {

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicMangers;
    private final JDA jda = JDABuilder.createDefault("NzkwMjEyODQxNDQ5MDYyNDEw.X99VDg.bfpnrkjQJy8U5PJ3ybN9bieuC4I").build();
    private final String url = "https://www.youtube.com/watch?v=pLuNy8qfK9Q";

    public static void main(String[] args) {
        TestBot tb;
        try {
             tb = new TestBot();
            tb.runBot();
        } catch (LoginException e) {
            e.printStackTrace();
        }


    }



    public TestBot() throws LoginException {


        musicMangers = new HashMap<>();
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        System.out.println("Bot is online");

        stopBot();
    }


        void runBot() throws LoginException {
            jda.getPresence().setActivity(Activity.watching("Lukas fail at programming"));
            jda.addEventListener(new TestBot());


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

        switch (message){
            case "-purge"->loadAndPlay(event.getChannel());
            case "-stop"->skipTrack(event.getChannel());
            case "-kys"->terminateBot(event.getChannel());
        }

    }

    private void terminateBot(final TextChannel channel) {
        new Thread(()->{
            System.out.println("Killing myself...");
            jda.getPresence().setStatus(OnlineStatus.OFFLINE);
            channel.sendMessage("ok :(").queue();
            jda.shutdown();

            System.exit(0);
        }).start();

    }

    private void loadAndPlay(final TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        playerManager.loadItemOrdered(musicManager, url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                channel.sendMessage("PURGING").queue();

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
                channel.sendMessage("Nothing found by " + url).queue();
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

    private void skipTrack(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.scheduler.nextTrack();

        channel.sendMessage("End Purge").queue();
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
                        jda.getPresence().setStatus(OnlineStatus.OFFLINE);
                        jda.shutdown();
                        System.out.println("bot offline");
                        reader.close();
                        System.exit(0);
                    }
                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}

