package abopijservice.code.songgeneration.song.generation.acestep;

import abopijservice.code.songgeneration.song.generation.request.AceStepGenerationRequest;
import abopijservice.code.songgeneration.song.generation.response.SongGenerationResponse;

public interface AceStepClient {

    SongGenerationResponse generateSong(AceStepGenerationRequest request);
}
