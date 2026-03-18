package staysplit.hotel_reservation.reservation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

@Slf4j
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
        log.info("[임시 예약 시작]");
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        HotelEntity hotel = hotelValidator.validateHotel(request.hotelId());

        // 참여자 리스트 처리 (자신 + 초대한 사람
        List<CustomerEntity> participants = getCustomerEntityFromUsernames(customer, request);

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
        long totalPrice = reserveRooms(request, reservation, nights, checkin, checkout);
        reservation.updateTotalPrice(totalPrice);

        log.debug("[총 금액을 참여자 수 만큼 나누기] totalPrice={}, numberOfParticipants={}", totalPrice, participants.size());
        long splitAmount = totalPrice / participants.size();

        // CustomerEntity -> ParticipantEntity 로 저장
        registerParticipants(reservation, participants, splitAmount);

        log.info("[임시 예약 성공] reservationId={}", reservation.getId());
        return mapper.toReservationDetailResponse(reservation);
    }

    // 모든 참여자들이 결제를 완료하면 예약 확정
    public ReservationDetailResponse confirmReservationAfterPayment(Integer reservationId) {
        ReservationEntity reservation = validateReservation(reservationId);

        boolean allPaid = reservation.getParticipants().stream()
                        .allMatch(p -> p.isPaid());

        if (!allPaid) {
            throw new AppException(ErrorCode.PAYMENT_INCOMPLETE_FOR_ALL_PARTICIPANTS);
        }
        reservation.markConfirmed();
        return mapper.toReservationDetailResponse(reservation);
    }

    // 예약 취소
    public void cancelReservation(String email, Integer reservationId) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        ReservationEntity reservation = validateReservation(reservationId);

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new AppException(ErrorCode.RESERVATION_NOT_FOUND, "이미 취소된 예약입니다.");
        }
        reservation.markCancelled();
    }

    private void registerParticipants(ReservationEntity reservation, List<CustomerEntity> customers, long splitAmount) {
        List<ReservationParticipantEntity> newParticipants = customers.stream()
                .map(customer -> ReservationParticipantEntity.builder()
                        .customer(customer)
                        .reservation(reservation)
                        .splitAmount(splitAmount)
                        .build())
                .toList();

        newParticipants.forEach(reservation::addParticipant);
        participantRepository.saveAll(newParticipants);
    }

    private List<CustomerEntity> getCustomerEntityFromUsernames(CustomerEntity owner, CreateReservationRequest request) {
        List<CustomerEntity> customerEntities = new ArrayList<>();
        if (request.nicknames() != null) {
            customerEntities = request.nicknames().stream()
                    .map(n -> {
                        log.info("[참여자 추가] nickname=[}", n);
                        return customerValidator.validateCustomerByNickname(n);
                    }).collect(Collectors.toList());
        }
        log.info("[초대된 사용자 수 = {}]", customerEntities.size());
        customerEntities.add(owner);
        return customerEntities;
    }

    private long reserveRooms(CreateReservationRequest request, ReservationEntity reservation, int nights,
                              LocalDate checkin, LocalDate checkout) {
        long totalPrice = 0L;

        // 요청된 방 하나씩 예약
        for (RoomReservationRequest roomDto : request.roomsAndQuantities()) {
            RoomEntity room = roomRepository.findByIdWithLock(roomDto.roomId())
                    .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

            // 이 방이 지정된 체크아웃과 체크인 날자 동안 재고가 있는지 확인
            int reservedCount = reservedRoomRepository.countReservedRoomsForDateRange(room.getId(), checkin, checkout);
            int available = room.getTotalQuantity() - reservedCount;
            if (available < roomDto.quantity()) {
                throw new AppException(ErrorCode.INSUFFICIENT_ROOM_STOCK);
            }

            // subtotal 계산
            long subtotal = (long) room.getPrice() * roomDto.quantity() * nights;
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
            throw new AppException(ErrorCode.INVALID_CHECKOUT_DATE);
        }
        return nights;
    }

    private ReservationEntity validateReservation(Integer reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESERVATION_NOT_FOUND));

    }

    private RoomEntity validateRoomById(Integer roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

}
