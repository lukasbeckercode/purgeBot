import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.audio.AudioSendHandler;

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
