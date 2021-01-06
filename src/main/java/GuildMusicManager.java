import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.audio.AudioSendHandler;
//This is one of those things i just copied and pasted
//It is necessary for the code to work, but i can´t be bothered to figure out how any of this works
public class GuildMusicManager {
    public final AudioPlayer player;
    public final AudioTrackScheduler scheduler;
    public GuildMusicManager(AudioPlayerManager manager) {
        player = manager.createPlayer();
        scheduler = new AudioTrackScheduler(player);
        player.addListener(scheduler);
    }

    public AudioSendHandler getSendHandler(){
        return new AudioPlayerSendHandler(player);
    }
}
