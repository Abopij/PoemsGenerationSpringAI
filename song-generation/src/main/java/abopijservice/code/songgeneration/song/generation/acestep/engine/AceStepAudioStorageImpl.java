package abopijservice.code.songgeneration.song.generation.acestep.engine;

import abopijservice.code.songgeneration.minio.MinIOService;
import abopijservice.code.songgeneration.song.generation.acestep.AceStepAudioStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AceStepAudioStorageImpl implements AceStepAudioStorage {

    private final MinIOService minIOService;

    @Override
    public List<String> store(String taskId, List<String> audioUrls, String fallbackFormat) {
        List<String> minioKeys = new ArrayList<>();
        for (int i = 0; i < audioUrls.size(); i++) {
            minioKeys.add(storeDataUrl(taskId, audioUrls.get(i), i + 1, fallbackFormat));
        }
        return minioKeys;
    }

    private String storeDataUrl(String taskId, String dataUrl, int index, String fallbackFormat) {
        DecodedAudio decodedAudio = decodeDataUrl(dataUrl, fallbackFormat);
        String objectName = "songs/" + safeObjectName(taskId) + "-" + index + "-" + UUID.randomUUID()
                + decodedAudio.extension();

        if (decodedAudio.bytes().length == 0) {
            throw new IllegalStateException("Downloaded empty audio file for task " + taskId);
        }

        try (InputStream stream = new ByteArrayInputStream(decodedAudio.bytes())) {
            minIOService.uploadStream(stream, objectName, decodedAudio.bytes().length);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to upload audio to MinIO for task " + taskId, e);
        }

        return objectName;
    }

    private static DecodedAudio decodeDataUrl(String dataUrl, String fallbackFormat) {
        if (!StringUtils.hasText(dataUrl)) {
            throw new IllegalStateException("ACE Music returned an empty audio data URL");
        }
        int comma = dataUrl.indexOf(',');
        if (comma < 0) {
            throw new IllegalStateException("ACE Music audio response is not a data URL");
        }

        String header = dataUrl.substring(0, comma);
        String payload = dataUrl.substring(comma + 1);
        byte[] bytes = Base64.getDecoder().decode(payload);
        return new DecodedAudio(bytes, resolveExtension(header, fallbackFormat));
    }

    private static String resolveExtension(String dataUrlHeader, String fallbackFormat) {
        if (StringUtils.hasText(dataUrlHeader)) {
            if (dataUrlHeader.contains("audio/wav")) return ".wav";
            if (dataUrlHeader.contains("audio/flac")) return ".flac";
            if (dataUrlHeader.contains("audio/mpeg") || dataUrlHeader.contains("audio/mp3")) return ".mp3";
        }

        String format = StringUtils.hasText(fallbackFormat) ? fallbackFormat.toLowerCase() : "mp3";
        return switch (format) {
            case "wav" -> ".wav";
            case "flac" -> ".flac";
            default -> ".mp3";
        };
    }

    private static String safeObjectName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private record DecodedAudio(byte[] bytes, String extension) {
    }
}
