package org.example.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Reads the pre-aggregated history file that the ETL script uploaded.
 * The bucket stays private: only this EC2 instance's IAM role can read it.
 */
@Service
public class HistoryService {

    private final S3Client s3 = S3Client.create();

    @Value("${app.history-bucket}")
    private String bucket;

    /** Returns the raw JSON produced by ghcn_etl.py, or null if absent. */
    public String getStationHistory(String stationId) {
        try {
            return s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key("history/" + stationId + ".json")
                    .build()).asUtf8String();
        } catch (NoSuchKeyException e) {
            return null;
        }
    }
}
