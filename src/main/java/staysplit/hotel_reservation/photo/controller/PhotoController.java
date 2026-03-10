package staysplit.hotel_reservation.photo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import staysplit.hotel_reservation.common.entity.Response;
import staysplit.hotel_reservation.photo.dto.response.PhotoDetailResponse;
import staysplit.hotel_reservation.photo.service.PhotoUploadService;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/photos")
public class PhotoController {

    private final PhotoUploadService photoUploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<PhotoDetailResponse> uploadPhoto(@RequestParam("entityType") String entityType,
                                                     @RequestParam("entityId") Integer entityId,
                                                     @RequestParam("displayType") String displayType,
                                                     @RequestPart("file") MultipartFile multipartFile,
                                                     Authentication authentication) throws IOException {
        PhotoDetailResponse photoDetailResponse = photoUploadService.uploadPhoto(
                authentication.getName(), entityType, entityId, displayType, multipartFile);
        return Response.success(photoDetailResponse);
    }

    @PostMapping(value = "/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<List<PhotoDetailResponse>> uploadPhoto(@RequestParam("entityType") String entityType,
                                                           @RequestParam("entityId") Integer entityId,
                                                           @RequestParam("displayType") List<String> displayTypes,
                                                           @RequestPart("files") List<MultipartFile> multipartFiles,
                                                           Authentication authentication) throws IOException {
        List<PhotoDetailResponse> responseList = photoUploadService.uploadPhotos(
                authentication.getName(), entityType, entityId, displayTypes, multipartFiles);
        return Response.success(responseList);
    }

    // 이미지 삭제
    @DeleteMapping("/{filename}")
    public Response<String> deleteImage(@PathVariable String filename) {
        photoUploadService.deletePhoto(filename);
        return Response.success("파일이 삭제되었습니다.");
    }

}
