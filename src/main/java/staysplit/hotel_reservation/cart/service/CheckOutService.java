package staysplit.hotel_reservation.cart.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staysplit.hotel_reservation.cart.domain.CartItemEntity;
import staysplit.hotel_reservation.cart.dto.request.CheckOutRequest;
import staysplit.hotel_reservation.cart.dto.response.CheckOutCartResponse;
import staysplit.hotel_reservation.cart.repository.CartItemRepository;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationKey;
import staysplit.hotel_reservation.reservation.dto.request.CreateReservationRequest;
import staysplit.hotel_reservation.reservation.dto.request.RoomReservationRequest;
import staysplit.hotel_reservation.reservation.dto.response.ReservationDetailResponse;
import staysplit.hotel_reservation.reservation.service.ReservationService;
import staysplit.hotel_reservation.reservedRoom.service.RoomStockService;
import staysplit.hotel_reservation.room.domain.RoomEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class CheckOutService {

    private final CartItemRepository cartItemRepository;
    private final CartValidator cartValidator;
    private final RoomStockService roomStockService;
    private final ReservationService reservationService;

    // 여러 호텔의 여러 방을 한번에 예약 할 수 있음
    public CheckOutCartResponse checkOut(String email, CheckOutRequest checkOutRequest) {

        Map<ReservationKey, List<CartItemEntity>> groups = new HashMap<>();
        for (Integer cartItemId: checkOutRequest.cartItemIds()) {
            CartItemEntity cartItem = cartValidator.validateCartItemById(cartItemId);
            RoomEntity room = cartItem.getRoom();

            // 재고 확인
            roomStockService.validateAvailableStock(room, cartItem.getCheckInDate(), cartItem.getCheckOutDate(), cartItem.getQuantity());

            ReservationKey reservationKey = new ReservationKey(room.getHotel().getId(), cartItem.getCheckInDate(), cartItem.getCheckOutDate());
            if (!groups.containsKey(reservationKey)) {
                groups.put(reservationKey, new ArrayList<>());
            }
            groups.get(reservationKey).add(cartItem);
        }

        // per reservation
        return groupAndCreateTempReservations(groups, email, checkOutRequest.invitedEmails(), checkOutRequest.isSplitPayment());
    }

    private CheckOutCartResponse groupAndCreateTempReservations(Map<ReservationKey, List<CartItemEntity>> groups,
                                                                           String email,
                                                                           List<String> invitedEmails,
                                                                           Boolean isSplitPayment) {

        List<ReservationDetailResponse> reservationList = new ArrayList<>();
        Integer totalPrice = 0;
        for (Map.Entry<ReservationKey, List<CartItemEntity>> entry : groups.entrySet()) {

            ReservationKey key = entry.getKey();
            List<CartItemEntity> cartItems = entry.getValue();

            List<RoomReservationRequest> roomRequests = new ArrayList<>();
            for (CartItemEntity cartItem: cartItems) {
                roomRequests.add(new RoomReservationRequest(cartItem.getRoom().getId(), cartItem.getQuantity()));
                totalPrice += cartItem.getSubTotal();
            }

            // create reservation
            CreateReservationRequest reservationRequest = createReservationRequest(key, invitedEmails, roomRequests, isSplitPayment);
            ReservationDetailResponse reservationDetailResponse = reservationService.makeTempReservation(email, reservationRequest);
            reservationList.add(reservationDetailResponse);

            cartItemRepository.deleteAll(cartItems);
        }
        return new CheckOutCartResponse(reservationList, totalPrice, isSplitPayment, totalPrice / (invitedEmails.size() + 1));
    }

    private CreateReservationRequest createReservationRequest(ReservationKey key,
                                                              List<String> invitedNicknames,
                                                              List<RoomReservationRequest> roomRequests,
                                                              Boolean isSplitPayment) {
        return CreateReservationRequest.builder()
                .hotelId(key.getHotelId())
                .roomsAndQuantities(roomRequests)
                .checkInDate(key.getCheckInDate())
                .checkOutDate(key.getCheckOutDate())
                .nicknames(invitedNicknames)
                .isSplitPayment(isSplitPayment)
                .build();
    }
}
