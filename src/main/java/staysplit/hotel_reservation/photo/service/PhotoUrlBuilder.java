package staysplit.hotel_reservation.photo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PhotoUrlBuilder {

    private final String basedUrl;

    public PhotoUrlBuilder(@Value("${app.backend-url}") String basedUrl) {
        this.basedUrl = basedUrl;
    }

    public String buildPhotoUrl(String filename) {
        return basedUrl + "/api/photos/" + filename;
    }
}
