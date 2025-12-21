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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import staysplit.hotel_reservation.cart.service.CartService;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.customer.domain.dto.request.CustomerSignupRequest;
import staysplit.hotel_reservation.customer.domain.dto.request.NicknameChangeRequest;
import staysplit.hotel_reservation.customer.domain.dto.response.CustomerDetailsResponse;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.repository.CustomerRepository;
import staysplit.hotel_reservation.customer.service.CustomerService;
import staysplit.hotel_reservation.reservation.service.UsernameAutocompleteService;
import staysplit.hotel_reservation.user.domain.entity.UserEntity;
import staysplit.hotel_reservation.user.domain.enums.LoginSource;
import staysplit.hotel_reservation.user.domain.enums.Role;
import staysplit.hotel_reservation.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private CartService cartService;

    @Mock
    private UsernameAutocompleteService usernameAutocompleteService;

    @InjectMocks
    private CustomerService customerService;

    private UserEntity user;
    private CustomerEntity customer;

    private String name;
    private LocalDate birthdate;
    private String testEmail;
    private String nonexistentEmail;
    private String rawPassword;
    private String encodedPassword;
    private String testNickname;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        name = "Test User";
        testEmail = "test@example.com";
        nonexistentEmail = "nonexistent@example.com";
        birthdate = LocalDate.of(1990, 1, 1);
        rawPassword = "12345";
        encodedPassword = "encodedPassword";
        testNickname = "testNickname";

        user = UserEntity.builder()
                .email(testEmail)
                .password(rawPassword)
                .role(Role.CUSTOMER)
                .build();

        customer = CustomerEntity.builder()
                .user(user)
                .name(name)
                .birthdate(birthdate)
                .nickname(testNickname)
                .build();
    }

    @Nested
    @DisplayName("Customer 회원가입")
    class CustomerSignUp {

        @Test
        @DisplayName("성공")
        public void signup_createsCustomerAndCart() {
            CustomerSignupRequest request = new CustomerSignupRequest(
                    testEmail,
                    rawPassword,
                    name,
                    birthdate,
                    testNickname);

            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(customerRepository.existsByNickname(request.nickname())).willReturn(false);
            given(passwordEncoder.encode(request.password())).willReturn(encodedPassword);

            CustomerDetailsResponse response = customerService.signup(request);

            // UserEntity 검증
            // userEntity가 생성되고 저장되었다.
            // 그때 어떤 UserEntity가 save()에 전달되었는지 저장하기 위한 준비단계
            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);

            // userRepository.save가 호출되어야 하고, 그때 전달된 UserEntity를 userCaptor에 저장하기
            then(userRepository).should().save(userCaptor.capture());

            // 저장된 UserEntity 검증
            UserEntity savedUser = userCaptor.getValue();
            assertThat(savedUser.getEmail()).isEqualTo(request.email());
            assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
            assertThat(savedUser.getRole()).isEqualTo(Role.CUSTOMER);
            assertThat(savedUser.getLoginSource()).isEqualTo(LoginSource.LOCAL);

            // CustomerEntity 검증
            // CustomerEntity가 생성되고 저장됨
            ArgumentCaptor<CustomerEntity> customerCaptor = ArgumentCaptor.forClass(CustomerEntity.class);

            // customerRepsoitory.save()로 저장된 인자 저장
            then(customerRepository).should().save(customerCaptor.capture());

            // 저장된 CustomerEntity 검증
            CustomerEntity savedCustomer = customerCaptor.getValue();
            assertThat(savedCustomer.getUser()).isNotNull();
            assertThat(savedCustomer.getUser().getEmail()).isEqualTo(testEmail);
            assertThat(savedCustomer.getName()).isEqualTo(name);
            assertThat(savedCustomer.getBirthdate()).isEqualTo(birthdate);
            assertThat(savedCustomer.getNickname()).isEqualTo(testNickname);

            // API 결과값 검증
            assertThat(response.nickname()).isEqualTo(testNickname);
            assertThat(response.email()).isEqualTo(testEmail);
        }

        @Test
        @DisplayName("실패 - 이메일 중복")
        void signup_throwsWhenEmailExists() {
            CustomerSignupRequest request = new CustomerSignupRequest(
                    "duplicateEmail@exmaple.com",
                    rawPassword,
                    name,
                    birthdate,
                    testNickname);

            given(userRepository.existsByEmail(request.email())).willReturn(true);

            assertThatThrownBy(() -> customerService.signup(request))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

            // 중복  체크는 했는지 확인
            then(userRepository).should().existsByEmail(request.email());

            // 그 이후로 실행되면 안되는 methods
            then(customerRepository).shouldHaveNoInteractions();
            then(passwordEncoder).shouldHaveNoInteractions();
            then(cartService).shouldHaveNoInteractions();
            then(usernameAutocompleteService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("실패 - username 중복")
        void signup_throwsWhenNicknameExists() {
            CustomerSignupRequest request = new CustomerSignupRequest(
                    testEmail,
                    rawPassword,
                    name,
                    birthdate,
                    "duplicateNickname");

            given(userRepository.existsByEmail(testEmail)).willReturn(false);
            given(customerRepository.existsByNickname("duplicateNickname")).willReturn(true);

            assertThatThrownBy(() -> customerService.signup(request))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);

            then(customerRepository).should().existsByNickname(request.nickname());
            then(userRepository).should(never()).save(any(UserEntity.class));
            then(passwordEncoder).shouldHaveNoInteractions();
            then(cartService).shouldHaveNoInteractions();
            then(usernameAutocompleteService).shouldHaveNoInteractions();
        }
    }

    @Test
    @DisplayName("My Profile 조회 성공")
    void getMyProfile_success() {
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(user));
        given(customerRepository.findByUser(user)).willReturn(Optional.of(customer));

        CustomerDetailsResponse response = customerService.getMyProfile(testEmail);

        assertThat(response.nickname()).isEqualTo(testNickname);
        assertThat(response.name()).isEqualTo(name);
        assertThat(response.email()).isEqualTo(testEmail);
    }

    @Nested
    @DisplayName("nickname 변경")
    class NicknameChange {

        @Test
        @DisplayName("성공")
        void changeNickname_updatesCustomerAndAutocomplete() {
            String newNickname = "newNickname";

            given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(user));
            given(customerRepository.findByUser(user)).willReturn(Optional.of(customer));
            given(customerRepository.existsByNickname(newNickname)).willReturn(false);

            NicknameChangeRequest request = new NicknameChangeRequest(newNickname);
            CustomerDetailsResponse response = customerService.changeNickname(request, testEmail);

            then(usernameAutocompleteService).should().removeUsername(testNickname);
            then(usernameAutocompleteService).should().addUsername(newNickname);

            assertThat(response.nickname()).isEqualTo(newNickname);
        }

        @Test
        @DisplayName("실패 - nickname 중복")
        void changeNickname_fail() {
            String duplicateNickname = "duplicateNickname";

            given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(user));
            given(customerRepository.findByUser(user)).willReturn(Optional.of(customer));
            given(customerRepository.existsByNickname(duplicateNickname)).willReturn(true);

            NicknameChangeRequest request = new NicknameChangeRequest(duplicateNickname);

            assertThatThrownBy(() -> customerService.changeNickname(request, testEmail))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_NICKNAME);

            then(customerRepository).should().existsByNickname(duplicateNickname);
            then(usernameAutocompleteService).shouldHaveNoInteractions();
            assertThat(customer.getNickname()).isEqualTo(testNickname); // 변경 안됨
        }
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void delete_removesCustomerAndCart() {
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(user));
        given(customerRepository.findByUser(user)).willReturn(Optional.of(customer));

        customerService.delete(testEmail);

        then(usernameAutocompleteService).should().removeUsername(testNickname);
        then(cartService).should().deleteCart(testEmail);

        then(customerRepository).should().delete(customer);
    }

}
