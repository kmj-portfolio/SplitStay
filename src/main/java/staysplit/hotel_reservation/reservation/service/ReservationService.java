package staysplit.hotel_reservation.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.service.CustomerValidator;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.hotel.service.HotelValidator;
import staysplit.hotel_reservation.reservation.dto.request.CreateReservationRequest;
import staysplit.hotel_reservation.reservation.dto.request.RoomReservationRequest;
import staysplit.hotel_reservation.reservation.dto.response.ReservationDetailResponse;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationParticipantEntity;
import staysplit.hotel_reservation.reservation.mapper.ReservationMapper;
import staysplit.hotel_reservation.reservedRoom.entity.ReservedRoomEntity;
import staysplit.hotel_reservation.reservation.domain.enums.ReservationStatus;
import staysplit.hotel_reservation.reservation.domain.enums.PaymentStatus;
import staysplit.hotel_reservation.reservation.reposiotry.ReservationParticipantRepository;
import staysplit.hotel_reservation.reservation.reposiotry.ReservationRepository;
import staysplit.hotel_reservation.reservedRoom.repository.ReservedRoomRepository;
import staysplit.hotel_reservation.room.domain.RoomEntity;
import staysplit.hotel_reservation.room.repository.RoomRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservedRoomRepository reservedRoomRepository;
    private final ReservationParticipantRepository participantRepository;
    private final RoomRepository roomRepository;
    private final CustomerValidator customerValidator;
    private final HotelValidator hotelValidator;
    private final ReservationMapper mapper;

    public ReservationDetailResponse makeTempReservation(String email, CreateReservationRequest request) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        HotelEntity hotel = hotelValidator.validateHotel(request.hotelId());

        // 숙박일 수 계산
        LocalDate checkin = request.checkInDate();
        LocalDate checkout = request.checkOutDate();
        int nights = calculateNights(request.checkInDate(), request.checkOutDate());

        // ReservationEntity 만들기 (status = WAITING_PAYMENT)
        ReservationEntity reservation = ReservationEntity.builder()
                .reservationNumber(generateReservationNumber())
                .hotel(hotel)
                .checkInDate(checkin)
                .checkOutDate(checkout)
                .nights(nights)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        reservationRepository.save(reservation);

        // 방 예약 (ReservedRoomEntity 생성 후 저장)
        int totalPrice = reserveRooms(request, reservation, nights, checkin, checkout);
        reservation.updateTotalPrice(totalPrice);

        // 참여자 리스트 (자신 + 초대한 사람)
        List<String> participantEmails = prepareParticipantEmails(email, request);

        Integer splitAmount = totalPrice / participantEmails.size();

        // 참여자 emails -> ParticipantEntity 로 저장
        saveParticipants(reservation, participantEmails, splitAmount);
        return mapper.toReservationDetailResponse(reservation);
    }

    // 모든 참여자들이 결제를 완료하면 예약 확정
    public ReservationDetailResponse confirmReservationAfterPayment(Integer reservationId) {
        ReservationEntity reservation = validateReservation(reservationId);

        for (ReservationParticipantEntity participant : reservation.getParticipants()) {
            if (!participant.getPaymentStatus().equals(PaymentStatus.COMPLETE)) {
                throw new AppException(ErrorCode.PAYMENT_INCOMPLETE_FOR_ALL_PARTICIPANTS,
                        ErrorCode.PAYMENT_INCOMPLETE_FOR_ALL_PARTICIPANTS.getMessage());
            }
        }
        reservation.updateStatus(ReservationStatus.CONFIRMED);
        return mapper.toReservationDetailResponse(reservation);
    }

    // 예약 취소
    public void cancelReservation(String email, Integer reservationId) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        ReservationEntity reservation = validateReservation(reservationId);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new AppException(ErrorCode.RESERVATION_NOT_FOUND, "이미 취소된 예약입니다.");
        }
        reservation.updateStatus(ReservationStatus.CANCELLED);
    }

    private void saveParticipants(ReservationEntity reservation, List<String> participantEmails, Integer splitAmount) {
        for (String emailOfParticipant : participantEmails) {
            CustomerEntity participantCustomer = customerValidator.validateCustomerByEmail(emailOfParticipant);

            ReservationParticipantEntity participant = ReservationParticipantEntity.builder()
                    .customer(participantCustomer)
                    .reservation(reservation)
                    .splitAmount(splitAmount)
                    .paymentStatus(PaymentStatus.WAITING)
                    .build();

            participantRepository.save(participant);
            reservation.addParticipant(participant);
        }
    }

    private List<String> prepareParticipantEmails(String email, CreateReservationRequest request) {
        List<String> participantEmails = new ArrayList<>();
        participantEmails.add(email);
        if (request.invitedEmails() != null) {
            participantEmails.addAll(request.invitedEmails());
        }
        return participantEmails;
    }

    private int reserveRooms(CreateReservationRequest request, ReservationEntity reservation, int nights,
                             LocalDate checkin, LocalDate checkout) {
        int totalPrice = 0;

        // 요청된 방 하나씩 예약
        for (RoomReservationRequest roomDto : request.roomsAndQuantities()) {
            RoomEntity room = roomRepository.findByIdWithLock(roomDto.roomId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND, ErrorCode.ROOM_NOT_FOUND.getMessage()));

            // 이 방이 지정된 체크아웃과 체크인 날자 동안 재고가 있는지 확인
            int reservedCount = reservedRoomRepository.countReservedRoomsForDateRange(room.getId(), checkin, checkout);
            int available = room.getTotalQuantity() - reservedCount;
            if (available < roomDto.quantity()) {
                throw new AppException(ErrorCode.INSUFFICIENT_ROOM_STOCK, ErrorCode.INSUFFICIENT_ROOM_STOCK.getMessage());
            }

            // subtotal 계산
            int subtotal = room.getPrice() * roomDto.quantity() * nights;
            totalPrice += subtotal;

            // Room Entity로 저장
            ReservedRoomEntity reservedRoom = ReservedRoomEntity.builder()
                    .reservation(reservation)
                    .room(room)
                    .quantity(roomDto.quantity())
                    .pricePerNight(room.getPrice())
                    .nights(nights)
                    .subtotalPrice(subtotal)
                    .build();

            reservedRoomRepository.save(reservedRoom);
            reservation.addReservedRoom(reservedRoom);
        }
        return totalPrice;
    }


    private String generateReservationNumber() {
        return "RES" + System.currentTimeMillis();
     }


    private int calculateNights(LocalDate checkIn, LocalDate checkOut) {
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            throw new AppException(ErrorCode.INVALID_CHECKOUT_DATE, ErrorCode.INVALID_CHECKOUT_DATE.getMessage());
        }
        return nights;
    }

    private ReservationEntity validateReservation(Integer reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESERVATION_NOT_FOUND, ErrorCode.RESERVATION_KEY_NOT_FOUND.getMessage()));

    }

    private RoomEntity validateRoomById(Integer roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND, ErrorCode.ROOM_NOT_FOUND.getMessage()));
    }

}
