package staysplit.hotel_reservation.photo.service;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${spring.cloud.aws.region.static}")
    private String region;

    public static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp"
    );

    public void upload(String key, MultipartFile file) throws IOException {

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE, ErrorCode.INVALID_FILE_TYPE.getMessage());
        }

        s3Template.upload(bucket, key, file.getInputStream(),
                io.awspring.cloud.s3.ObjectMetadata.builder()
                        .contentType(file.getContentType())
                        .build());
    }

    public void delete(String key) {
        s3Template.deleteObject(bucket, key);
    }

    public String getS3Url(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }
}
