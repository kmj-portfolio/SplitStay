package staysplit.hotel_reservation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import staysplit.hotel_reservation.auth.dto.request.LoginRequest;
import staysplit.hotel_reservation.auth.dto.request.PasswordUpdateRequest;
import staysplit.hotel_reservation.auth.dto.response.LoginTokens;
import staysplit.hotel_reservation.auth.service.AuthService;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.common.security.jwt.JwtTokenProvider;
import staysplit.hotel_reservation.user.domain.entity.UserEntity;
import staysplit.hotel_reservation.user.domain.enums.Role;
import staysplit.hotel_reservation.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService userService;

    private String testEmail;
    private String nonexistentEmail;
    private String rawPassword;
    private String encodedPassword;

    private UserEntity testUser;

    @BeforeEach
    void setup() {
        testEmail = "test@example.com";
        nonexistentEmail = "nonexistent@example.com";
        rawPassword = "12345";
        encodedPassword = "encodedPassword";

        testUser = UserEntity.builder()
                .email(testEmail)
                .password(encodedPassword)
                .role(Role.CUSTOMER)
                .build();
    }

    @Nested
    @DisplayName("회원 로그인 테스트")
    class UserLogin {

        private final String ACCESS_TOKEN = "mock.access.token";
        private final String REFRESH_TOKEN = "mock.refresh.token";

        @Test
        @DisplayName("로그인 성공 테스트")
        void login_success_returnsToken() {
            // given
            LoginRequest loginRequest = new LoginRequest(testEmail, rawPassword);

            // when
            given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(rawPassword, encodedPassword)).willReturn(true);
            given(jwtTokenProvider.createAccessToken(testEmail)).willReturn(ACCESS_TOKEN);
            given(jwtTokenProvider.createRefreshToken(testEmail)).willReturn(REFRESH_TOKEN);

            // when
            LoginTokens response = userService.login(loginRequest);

            // then
            assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
            assertThat(response.role()).isEqualTo("CUSTOMER");
            then(userRepository).should().findByEmail(testEmail);
            then(jwtTokenProvider).should().createAccessToken(testEmail);
            then(jwtTokenProvider).should().createRefreshToken(testEmail);
        }

        @Test
        @DisplayName("로그인 실패 - 가입 되지 않은 회원")
        void login_userNotFound_throwsException() {
            // given
            LoginRequest loginRequest = new LoginRequest(nonexistentEmail, rawPassword);
            given(userRepository.findByEmail(loginRequest.email())).willReturn(Optional.empty());

            // when + then
            assertThatThrownBy(() -> userService.login(loginRequest))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
            then(userRepository).should().findByEmail(loginRequest.email());
        }

        @Test
        @DisplayName("로그인 실패 - 비밀번호 불일치")
        void login_invalidPassword_throwsException() {
            // given
            LoginRequest loginRequest = new LoginRequest(testEmail, "wrongPassword");

            given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(loginRequest.password(), encodedPassword)).willReturn(false);

            // expect
            assertThatThrownBy(() -> userService.login(loginRequest))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.INVALID_PASSWORD.getMessage());

            then(userRepository).should().findByEmail(testEmail);
            then(passwordEncoder).should().matches(loginRequest.password(), encodedPassword);
        }
    }

    @Nested
    @DisplayName("회원 비밀번호 변경 테스트")
    class ChangePassword {

        private final String newPassword = "newPassword";

        @Test
        @DisplayName("성공")
        public void success() {
            given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));
            given(passwordEncoder.encode(newPassword)).willReturn("newEncodedPassword");

            String result = userService.changePassword(new PasswordUpdateRequest(rawPassword, newPassword), testEmail);

            assertThat(result).isEqualTo("비밀번호가 변경되었습니다.");
            then(userRepository).should().findByEmail(testEmail);
            then(passwordEncoder).should().encode(newPassword);

            // UserEntity의 비밀번호 변경 되었는지 검증
            assertThat(testUser.getPassword()).isEqualTo("newEncodedPassword");
        }

        @Test
        @DisplayName("실패 - 가입되지 않은 회원인 경우")
        public void failure_nonexistent_user() {
            given(userRepository.findByEmail(nonexistentEmail)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changePassword(
                    new PasswordUpdateRequest(rawPassword, newPassword), nonexistentEmail))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());

            then(userRepository).should().findByEmail(nonexistentEmail);
            then(passwordEncoder).shouldHaveNoInteractions();
        }
    }
}
