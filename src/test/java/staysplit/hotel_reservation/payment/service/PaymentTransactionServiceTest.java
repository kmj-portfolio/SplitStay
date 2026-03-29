
package staysplit.hotel_reservation.payment.service;

import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.repository.CustomerRepository;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.hotel.repository.HotelRepository;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;
import staysplit.hotel_reservation.payment.domain.enums.PaymentStatus;
import staysplit.hotel_reservation.payment.repository.PaymentRepository;
import staysplit.hotel_reservation.provider.domain.entity.ProviderEntity;
import staysplit.hotel_reservation.provider.repository.ProviderRepository;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationParticipantEntity;
import staysplit.hotel_reservation.reservation.reposiotry.ReservationParticipantRepository;
import staysplit.hotel_reservation.reservation.reposiotry.ReservationRepository;
import staysplit.hotel_reservation.user.domain.entity.UserEntity;
import staysplit.hotel_reservation.user.domain.enums.LoginSource;
import staysplit.hotel_reservation.user.domain.enums.Role;
import staysplit.hotel_reservation.user.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
class PaymentTransactionServiceTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("concurrency_test_db")
            .withUsername("testuser")
            .withPassword("test");

    @MockitoBean
    private S3Template s3Template;

    @Autowired PlatformTransactionManager platformTransactionManager;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PaymentTransactionService paymentTransactionService;
    @Autowired PaymentRepository paymentRepository;
    @Autowired ReservationRepository reservationRepository;
    @Autowired ReservationParticipantRepository participantRepository;
    @Autowired HotelRepository hotelRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired UserRepository userRepository;
    @Autowired ProviderRepository providerRepository;

    private static final int THREAD_COUNT = 100;
    private static final String PAYMENT_ID = "payment-123456";
    private static final long SPLIT_AMOUNT = 10_000L;

    private Integer savedReservationId;

    @BeforeEach
    void setup() {
        savedReservationId = new TransactionTemplate(platformTransactionManager).execute(status -> {
            UserEntity providerUser = userRepository.save(UserEntity.builder()
                    .email("provider@test.com")
                    .loginSource(LoginSource.LOCAL)
                    .role(Role.PROVIDER)
                    .build());

            ProviderEntity provider = providerRepository.save(ProviderEntity.builder()
                    .user(providerUser)
                    .build());

            HotelEntity hotel = hotelRepository.save(HotelEntity.builder()
                    .provider(provider)
                    .name("Test Hotel")
                    .address("Seoul")
                    .longitude(127.0)
                    .latitude(37.5)
                    .build());

            provider.addHotel(hotel);

            UserEntity customerUser = userRepository.save(UserEntity.builder()
                    .email("customer@test.com")
                    .loginSource(LoginSource.LOCAL)
                    .role(Role.CUSTOMER)
                    .build());

            CustomerEntity customer = customerRepository.save(CustomerEntity.builder()
                    .user(customerUser)
                    .name("Test Customer")
                    .birthdate(LocalDate.of(1990, 1, 1))
                    .phoneNumber("01012345678")
                    .build());

            ReservationEntity reservation = reservationRepository.save(ReservationEntity.builder()
                    .hotel(hotel)
                    .reservationNumber("RES-CONCURRENCY-001")
                    .nights(1)
                    .checkInDate(LocalDate.of(2026, 3, 28))
                    .checkOutDate(LocalDate.of(2026, 3, 29))
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .totalPrice(SPLIT_AMOUNT)
                    .build());

            ReservationParticipantEntity participant = participantRepository.save(
                    ReservationParticipantEntity.builder()
                            .reservation(reservation)
                            .customer(customer)
                            .splitAmount(SPLIT_AMOUNT)
                            .build());

            paymentRepository.save(PaymentEntity.builder()
                    .reservationParticipant(participant)
                    .paymentId(PAYMENT_ID)
                    .amount(SPLIT_AMOUNT)
                    .status(PaymentStatus.READY)
                    .build());

            return reservation.getId();
        });
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        paymentRepository.deleteAll();
        participantRepository.deleteAll();
        reservationRepository.deleteAll();
        hotelRepository.deleteAll();
        providerRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
    }

    @Test
    @DisplayName("동시에 100개의 thread가 동일한 결제에 대한 confirmation을 요청해도 한 번만 처리된다")
    void testConcurrency() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch end = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    start.await();
                    paymentTransactionService.processConfirmation(PAYMENT_ID);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    end.countDown();
                }
            });
        }

        start.countDown();
        end.await();

        executorService.shutdown();

        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
        }

        PaymentEntity result = paymentRepository.findByPaymentId(PAYMENT_ID).orElseThrow();
        assertTrue(result.isPaid());

        ReservationEntity reservation = reservationRepository.findById(savedReservationId).orElseThrow();
        assertEquals(SPLIT_AMOUNT, reservation.getPricePaid());
    }
}
