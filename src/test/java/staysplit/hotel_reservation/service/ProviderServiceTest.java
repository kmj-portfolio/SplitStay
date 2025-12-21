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
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.provider.domain.dto.reqeust.ProviderSignupRequest;
import staysplit.hotel_reservation.provider.domain.dto.response.ProviderSignupResponse;
import staysplit.hotel_reservation.provider.domain.entity.ProviderEntity;
import staysplit.hotel_reservation.provider.repository.ProviderRepository;
import staysplit.hotel_reservation.provider.service.ProviderService;
import staysplit.hotel_reservation.user.domain.entity.UserEntity;
import staysplit.hotel_reservation.user.domain.enums.LoginSource;
import staysplit.hotel_reservation.user.domain.enums.Role;
import staysplit.hotel_reservation.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private ProviderService providerService;

    private String email;
    private String rawPassword;
    private String encodedPassword;

    @BeforeEach
    void setUp() {
        email = "provider@example.com";
        rawPassword = "12345";
        encodedPassword = "encodedPassword";
    }

    @Nested
    @DisplayName("Provider 회원 가입")
    class ProviderSignup {

        @Test
        @DisplayName("성공")
        void signup_successful() {
            ProviderSignupRequest request = new ProviderSignupRequest(email, rawPassword);

            given(userRepository.existsByEmail(request.email())).willReturn(false);
            given(passwordEncoder.encode(request.password())).willReturn(encodedPassword);

            ProviderSignupResponse response = providerService.signup(request);

            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            ArgumentCaptor<ProviderEntity> providerCaptor = ArgumentCaptor.forClass(ProviderEntity.class);
            then(userRepository).should().save(userCaptor.capture());
            then(providerRepository).should().save(providerCaptor.capture());

            UserEntity savedUser = userCaptor.getValue();
            assertThat(savedUser.getEmail()).isEqualTo(email);
            assertThat(savedUser.getPassword()).isEqualTo(encodedPassword);
            assertThat(savedUser.getRole()).isEqualTo(Role.PROVIDER);
            assertThat(savedUser.getLoginSource()).isEqualTo(LoginSource.LOCAL);

            ProviderEntity savedProvider = providerCaptor.getValue();
            assertThat(savedProvider.getUser()).isSameAs(savedUser);

            assertThat(response.email()).isEqualTo(email);
        }

        @Test
        @DisplayName("실패 - 이메일 중복")
        void signup_fail_duplicateEmail() {
            ProviderSignupRequest request = new ProviderSignupRequest(email, rawPassword);

            given(userRepository.existsByEmail(email)).willReturn(true);

            assertThatThrownBy(() -> providerService.signup(request))
                    .isInstanceOf(AppException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.DUPLICATE_EMAIL);

            then(userRepository).should().existsByEmail(request.email());
            then(userRepository).should(never()).save(any(UserEntity.class));
            then(providerRepository).shouldHaveNoInteractions();
        }
    }
}