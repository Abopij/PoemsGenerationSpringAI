package abopijservice.code.songgeneration.song.generation.acestep;

import java.util.List;

public interface AceStepAudioStorage {

    List<String> store(String taskId, List<String> audioUrls, String fallbackFormat);
}
