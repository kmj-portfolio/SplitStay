package staysplit.hotel_reservation.photo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PhotoUrlBuilder {

    private final S3Service s3Service;

    public String buildPhotoUrl(String filename) {
        return s3Service.getS3Url(filename);
    }
}
