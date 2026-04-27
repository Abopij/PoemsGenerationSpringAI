package abopijservice.code.songgeneration.minio;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class MinIOService {

    private static final String BUCKET_NAME = "files"; // TODO replaced to normal bucket for any user by him email addr

    private final MinioClient minioClient;

    @SneakyThrows
    public void uploadStream(InputStream stream, String objectName, long sizeBytes) {
        ensureBucketExists();

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(objectName)
                        .stream(
                                stream,
                                sizeBytes,
                                -1
                        )
                        .contentType(
                                resolveContentType(
                                        objectName
                                )
                        )
                        .build()
        );
    }

    @SneakyThrows
    public InputStream downloadFile(String filename) {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(filename)
                        .build()
        );
    }

    @SneakyThrows
    public void deleteFile(String filename) {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(filename)
                        .build()
        );
    }

    @SneakyThrows
    private void ensureBucketExists() {
        boolean found = minioClient.bucketExists(
                BucketExistsArgs
                        .builder()
                        .bucket(BUCKET_NAME)
                        .build()
        );
        if (!found) {
            minioClient
                    .makeBucket(
                            MakeBucketArgs
                                    .builder()
                                    .bucket(BUCKET_NAME)
                                    .build()
                    );
        }
    }

    private static String resolveContentType(String filename) {
        if (filename.endsWith(".mp3")) return "audio/mpeg";
        if (filename.endsWith(".wav")) return "audio/wav";
        return "application/octet-stream";
    }
}
