package staysplit.hotel_reservation.customer.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.customer.domain.dto.request.NicknameChangeRequest;
import staysplit.hotel_reservation.customer.domain.dto.response.CustomerDetailsResponse;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.repository.CustomerRepository;
import staysplit.hotel_reservation.user.domain.entity.UserEntity;
import staysplit.hotel_reservation.user.domain.enums.Role;
import staysplit.hotel_reservation.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomerService customerService;

    private AutoCloseable closeable;

    private UserEntity user;
    private CustomerEntity customer;

    /*
    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        user = UserEntity.builder()
                .email("test@example.com")
                .password("encoded-password")
                .role(Role.CUSTOMER)
                .build();

        customer = CustomerEntity.builder()
                .user(user)
                .name("Test User")
                .birthdate(LocalDate.of(1990, 1, 1))
                .nickname("tester")
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }


    @Test
    void getMyProfile_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser(user)).thenReturn(Optional.of(customer));

        CustomerDetailsResponse result = customerService.getMyProfile("test@example.com");

        assertThat(result.nickname()).isEqualTo("tester");
    }

    @Test
    void findCustomerById_success() {
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));

        CustomerDetailsResponse result = customerService.findCustomerById(1);

        assertThat(result.name()).isEqualTo("Test User");
    }

    @Test
    void changeNickname_success() {
        NicknameChangeRequest request = new NicknameChangeRequest("newNick");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser(user)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByNickname("newNick")).thenReturn(false);

        CustomerDetailsResponse result = customerService.changeNickname(request, "test@example.com");

        assertThat(result.nickname()).isEqualTo("newNick");
    }

    @Test
    void changeNickname_fail() {
        NicknameChangeRequest request = new NicknameChangeRequest("tester");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(customerRepository.findByUser(user)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByNickname("tester")).thenReturn(true);

        assertThrows(AppException.class, () -> customerService.changeNickname(request, "test@example.com"));
    }*/

}
