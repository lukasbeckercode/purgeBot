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
/*
* This is a bot that plays the purge siren in discord
* I just copied stuff from github and it somehow works
* I have no idea how or why this works, but it does
* Yaay
* */


public class TestBot extends ListenerAdapter {

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicMangers;
    private final JDA jda = JDABuilder.createDefault("TOKEN").build();
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
        //Constructor

        musicMangers = new HashMap<>();
        playerManager = new DefaultAudioPlayerManager();

        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        System.out.println("Bot is online");

        stopBot();
    }


        void runBot() throws LoginException {
        //show stuff if the bot is running
            jda.getPresence().setActivity(Activity.watching("Lukas fail at programming"));
            jda.addEventListener(new TestBot());


        }


    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild){
        //???

        long guildID = Long.parseLong(guild.getId()); //get guild id

        GuildMusicManager musicManger = musicMangers.get(guildID); //new Object, set guild id

        if(musicManger == null){
            musicManger = new GuildMusicManager(playerManager);
            musicMangers.put(guildID,musicManger);
        }

        guild.getAudioManager().setSendingHandler(musicManger.getSendHandler());

        return musicManger;
    }
    public void onGuildMessageReceived(GuildMessageReceivedEvent event){
        //read incoming commands
        String message = event.getMessage().getContentDisplay();

        switch (message){
            case "-purge"->loadAndPlay(event.getChannel());
            case "-stop"->skipTrack(event.getChannel());
            case "-kys"->terminateBot(event.getChannel());
        }

    }

    private void terminateBot(final TextChannel channel) {
        //Not working properly
        new Thread(()->{
            System.out.println("Killing myself...");
            jda.getPresence().setStatus(OnlineStatus.OFFLINE);
            channel.sendMessage("ok :(").queue();
            jda.shutdown();

            System.exit(0);
        }).start();

    }

    private void loadAndPlay(final TextChannel channel) {
        //this plays the track
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild()); //play in the current channel

        playerManager.loadItemOrdered(musicManager, url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                //Load the track
                channel.sendMessage("PURGING").queue();

                play(channel.getGuild(), musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                //useless, because i only use 1 track
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
        //play the sound effect
            connectToFirstVoiceChannel(guild.getAudioManager());

            musicManager.scheduler.queue(track);
    }

    private void skipTrack(TextChannel channel) {
        //skip, if nothing follows the current playing track, stop
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.scheduler.nextTrack();

        channel.sendMessage("End Purge").queue();
    }

    private void connectToFirstVoiceChannel(AudioManager audioManager) {
        //This seems to look for the right voice channel
        //I don´t know for sure, this is just me pressing crtl+c and crtl+v repeatedly
        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) { //This seems to be deprecated :(
            for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
                audioManager.openAudioConnection(voiceChannel);
                break;
            }
        }
    }




    void stopBot() {
        //another attempt to stop the bot, works sometimes
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

