package abopijservice.code.songgeneration.song.generation.acestep.dto;

public record AceStepTextContentPart(
        String type,
        String text
) implements AceStepMessageContentPart {

    public AceStepTextContentPart(String text) {
        this("text", text);
    }
}
