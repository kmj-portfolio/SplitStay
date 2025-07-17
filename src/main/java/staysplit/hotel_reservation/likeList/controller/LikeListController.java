package staysplit.hotel_reservation.likeList.controller;

import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import staysplit.hotel_reservation.common.entity.Response;
import staysplit.hotel_reservation.likeList.dto.response.CreateLikeListResponse;
import staysplit.hotel_reservation.likeList.dto.response.LikeListCollectionResponse;
import staysplit.hotel_reservation.likeList.dto.response.LikeListDetailResponse;
import staysplit.hotel_reservation.likeList.service.LikeListService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/like-lists")
public class LikeListController {
    private final LikeListService likeListService;

    // 목록 조회
    @GetMapping("/{listId}")
    public Response<LikeListDetailResponse> getLikeList(Authentication authentication, @PathVariable Integer listId) {
        LikeListDetailResponse response = likeListService.getLikeList(authentication.getName(), listId);
        return Response.success(response);
    }

    // 목록 생성
    @PostMapping
    public Response<CreateLikeListResponse> createLikeList(Authentication authentication, @PathParam("listName") String listName) {
        CreateLikeListResponse response = likeListService.createLikeList(authentication.getName(), listName);
        return Response.success(response);
    }

    // 목록 삭제
    @DeleteMapping("/{listId}")
    public Response<String> deleteLikeList(Authentication authentication, @PathVariable Integer listId) {
        likeListService.deleteLikeList(authentication.getName(), listId);
        return Response.success("목록이 삭제되었습니다.");
    }

    // 목록 이름 변경
    @PutMapping("/{listId}")
    public Response<String> changeListName(Authentication authentication, @PathVariable Integer listId,
                                           @RequestParam("listName") String listName) {
        likeListService.changeListName(authentication.getName(), listId, listName);
        return Response.success("목록 이름이 변경되었습니다.");
    }

    // 사용자의 모든 좋아요 목록 조회
    @GetMapping("/customers")
    public Response<List<LikeListCollectionResponse>> findAllLikeListByCustomer(Authentication authentication) {
        List<LikeListCollectionResponse> response = likeListService.findAllByCustomer(authentication.getName());
        return Response.success(response);
    }
}
