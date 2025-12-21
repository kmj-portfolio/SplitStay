package staysplit.hotel_reservation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.service.CustomerValidator;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.hotel.service.HotelValidator;
import staysplit.hotel_reservation.provider.domain.entity.ProviderEntity;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationParticipantEntity;
import staysplit.hotel_reservation.reservation.domain.enums.ReservationStatus;
import staysplit.hotel_reservation.reservation.dto.request.CreateReservationRequest;
import staysplit.hotel_reservation.reservation.dto.request.RoomReservationRequest;
import staysplit.hotel_reservation.reservation.dto.response.ReservationDetailResponse;
import staysplit.hotel_reservation.reservation.mapper.ReservationMapper;
import staysplit.hotel_reservation.reservation.reposiotry.ReservationParticipantRepository;
import staysplit.hotel_reservation.reservation.reposiotry.ReservationRepository;
import staysplit.hotel_reservation.reservation.service.ReservationService;
import staysplit.hotel_reservation.reservedRoom.entity.ReservedRoomEntity;
import staysplit.hotel_reservation.reservedRoom.repository.ReservedRoomRepository;
import staysplit.hotel_reservation.room.domain.RoomEntity;
import staysplit.hotel_reservation.room.repository.RoomRepository;
import staysplit.hotel_reservation.user.domain.entity.UserEntity;
import staysplit.hotel_reservation.user.domain.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservedRoomRepository reservedRoomRepository;

    @Mock
    private ReservationParticipantRepository participantRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private CustomerValidator customerValidator;

    @Mock
    private HotelValidator hotelValidator;

    @Mock
    private ReservationMapper mapper;

    @InjectMocks
    private ReservationService reservationService;

    private CustomerEntity customer;
    private HotelEntity hotel;
    private RoomEntity room;

    @BeforeEach
    void setup() {
        UserEntity user = UserEntity.builder()
                .email("guest@example.com")
                .role(Role.CUSTOMER)
                .build();

        customer = CustomerEntity.builder()
                .id(1)
                .user(user)
                .name("Guest")
                .birthdate(LocalDate.of(1990, 1, 1))
                .nickname("guest")
                .build();

        UserEntity user2 = UserEntity.builder()
                .id(2)
                .email("provider@example.com")
                .role(Role.PROVIDER)
                .build();

        ProviderEntity provider = ProviderEntity.builder()
                .user(user2)
                .build();

        hotel = HotelEntity.builder()
                .id(1)
                .name("Test Hotel")
                .provider(provider)
                .address("Seoul")
                .longitude(127.0)
                .latitude(37.0)
                .build();

        room = RoomEntity.builder()
                .id(5)
                .hotel(hotel)
                .roomType("Standard")
                .maxOccupancy(2)
                .price(100)
                .description("Standard Room")
                .totalQuantity(3)
                .build();
    }

    @Nested
    @DisplayName("호텔 재고 확보")
    class Reservation {

        @Test
        @DisplayName("성공")
        void success() {
            CreateReservationRequest request = CreateReservationRequest.builder()
                    .hotelId(1)
                    .roomsAndQuantities(List.of(new RoomReservationRequest(5, 2)))
                    .checkInDate(LocalDate.of(2025, 12, 21))
                    .checkOutDate(LocalDate.of(2025, 12, 25))
                    .invitedEmails(List.of("friend@example.com"))
                    .isSplitPayment(true)
                    .build();

            given(customerValidator.validateCustomerByEmail("guest@example.com")).willReturn(customer);

            UserEntity friendUser = UserEntity.builder()
                    .id(3)
                    .email("friend@example.com")
                    .role(Role.CUSTOMER)
                    .build();

            CustomerEntity friendCustomer = CustomerEntity.builder()
                    .id(2)
                    .user(friendUser)
                    .name("friend")
                    .birthdate(LocalDate.of(1990, 1, 1))
                    .nickname("friend")
                    .build();

            given(customerValidator.validateCustomerByEmail("friend@example.com")).willReturn(friendCustomer);
            given(hotelValidator.validateHotel(1)).willReturn(hotel);
            given(roomRepository.findByIdWithLock(5)).willReturn(Optional.of(room));
            given(reservedRoomRepository.countReservedRoomsForDateRange(
                    5,
                    request.checkInDate(),
                    request.checkOutDate())).willReturn(0);

            // 검증할 method
            ReservationDetailResponse response = reservationService.makeTempReservation("guest@example.com", request);

            ArgumentCaptor<ReservationEntity> reservationCaptor = ArgumentCaptor.forClass(ReservationEntity.class);
            then(reservationRepository).should().save(reservationCaptor.capture());

            // ReservedRoom 생성 후 저장
            ArgumentCaptor<ReservedRoomEntity> reservedRoomCaptor = ArgumentCaptor.forClass(ReservedRoomEntity.class);
            then(reservedRoomRepository).should().save(reservedRoomCaptor.capture());

            // ReservationParticipant 생성 후 저장
            ArgumentCaptor<ReservationParticipantEntity> participantCaptor = ArgumentCaptor.forClass(ReservationParticipantEntity.class);
            then(participantRepository).should(times(2)).save(participantCaptor.capture());

            // mapper 호출
            then(mapper).should().toReservationDetailResponse(any(ReservationEntity.class));

            ReservedRoomEntity savedRoom = reservedRoomCaptor.getValue();
            assertThat(savedRoom.getQuantity()).isEqualTo(2);
            assertThat(savedRoom.getNights()).isEqualTo(4);
            assertThat(savedRoom.getSubtotalPrice()).isEqualTo(800); // 100 * 2 rooms * 4 days = 800

            participantCaptor.getAllValues().forEach(participant ->
                    assertThat(participant.getSplitAmount()).isEqualTo(400)); // 800 / 2 = 400

            ReservationEntity savedReservation = reservationCaptor.getValue();
            assertThat(savedReservation.getNights()).isEqualTo(4);
        }

        @Test
        @DisplayName("실패 - 방 부족")
        void makeTempReservation_throwsWhenRoomStockIsInsufficient() {
            CreateReservationRequest request = CreateReservationRequest.builder()
                    .hotelId(1)
                    .roomsAndQuantities(List.of(new RoomReservationRequest(5, 2)))
                    .checkInDate(LocalDate.of(2025, 12, 21))
                    .checkOutDate(LocalDate.of(2025, 12, 25))
                    .isSplitPayment(true)
                    .build();

            given(customerValidator.validateCustomerByEmail("guest@example.com")).willReturn(customer);
            given(hotelValidator.validateHotel(1)).willReturn(hotel);
            given(roomRepository.findByIdWithLock(5)).willReturn(Optional.of(room));
            given(reservedRoomRepository.countReservedRoomsForDateRange(
                    5,
                    request.checkInDate(),
                    request.checkOutDate())).willReturn(3); // 방 stock과 같음

            // 검증할 method
            assertThatThrownBy(() -> reservationService.makeTempReservation("guest@example.com", request))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INSUFFICIENT_ROOM_STOCK);

            then(reservedRoomRepository).should(never()).save(any(ReservedRoomEntity.class));
            then(participantRepository).should(never()).save(any(ReservationParticipantEntity.class));
        }
    }



    @Test
    @DisplayName("성공")
    void confirmReservationAfterPayment_success() {
        ReservationEntity reservation = ReservationEntity.builder()
                .id(1)
                .reservationNumber("RESERVATION-123")
                .hotel(hotel)
                .checkInDate(LocalDate.of(2025, 12, 21))
                .checkOutDate(LocalDate.of(2025, 12, 25))
                .nights(4)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        given(reservationRepository.findById(1)).willReturn(Optional.of(reservation));

        ReservationDetailResponse expectedResponse = ReservationDetailResponse.builder()
                .reservationId(1)
                .reservationStatus("CONFIRMED")
                .build();
        given(mapper.toReservationDetailResponse(reservation)).willReturn(expectedResponse);

        ReservationDetailResponse response = reservationService.confirmReservationAfterPayment(1);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED); // reservation status 변경됐는지 확인
        assertThat(response).isSameAs(expectedResponse);
    }

    @Test
    @DisplayName("취소")
    void cancelReservation() {
        ReservationEntity reservation = ReservationEntity.builder()
                .id(1)
                .reservationNumber("RESERVATION-123")
                .hotel(hotel)
                .checkInDate(LocalDate.of(2025, 12, 21))
                .checkOutDate(LocalDate.of(2025, 12, 25))
                .nights(4)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        given(customerValidator.validateCustomerByEmail("guest@example.com")).willReturn(customer);
        given(reservationRepository.findById(1)).willReturn(Optional.of(reservation));

        reservationService.cancelReservation("guest@example.com", 1);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName(("예약 취소 실패 - 중복 취소"))
    void cancelReservation_throwsWhenAlreadyCancelled() {
        ReservationEntity reservation = ReservationEntity.builder()
                .id(1)
                .reservationNumber("RESERVATION-123")
                .hotel(hotel)
                .checkInDate(LocalDate.of(2025, 12, 21))
                .checkOutDate(LocalDate.of(2025, 12, 25))
                .nights(4)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .status(ReservationStatus.CANCELLED)
                .build();

        given(customerValidator.validateCustomerByEmail("guest@example.com")).willReturn(customer);
        given(reservationRepository.findById(1)).willReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation("guest@example.com", 1))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
    }
}
