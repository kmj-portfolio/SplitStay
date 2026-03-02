package staysplit.hotel_reservation.likeList.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import staysplit.hotel_reservation.common.entity.Response;
import staysplit.hotel_reservation.likeList.dto.request.ParticipantRequest;
import staysplit.hotel_reservation.likeList.service.LikeListCustomerService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/like-lists")
public class LikeListCustomerController {

    private final LikeListCustomerService likeListCustomerService;

    @PostMapping("/{listId}/owners/participants")
    public Response<String> addParticipant(Authentication authentication,
                                           @PathVariable Integer listId,
                                           @RequestBody ParticipantRequest request) {
        likeListCustomerService.addCustomerToLikeList(authentication.getName(), listId, request.invitedEmail());
        return Response.success("사용자가 추가되었습니다.");
    }

    @DeleteMapping("/{listId}/owners/participants")
    public Response<String> removeParticipant(Authentication authentication,
                                              @PathVariable Integer listId,
                                              @RequestBody ParticipantRequest request) {
        likeListCustomerService.addCustomerToLikeList(authentication.getName(), listId, request.invitedEmail());
        return Response.success("사용자를 제거했습니다..");
    }

    @DeleteMapping("/{listId}/participants")
    public Response<String> leaveLikeList(Authentication authentication, @PathVariable Integer listId) {
        likeListCustomerService.leaveLikeList(authentication.getName(), listId);
        return Response.success("좋아요 목록에서 나왔습니다.");
    }
}
