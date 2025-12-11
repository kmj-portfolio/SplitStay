package staysplit.hotel_reservation.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import staysplit.hotel_reservation.auth.service.AuthService;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.common.security.jwt.JwtTokenProvider;
import staysplit.hotel_reservation.auth.dto.response.UserLoginResponse;
import staysplit.hotel_reservation.user.domain.entity.UserEntity;
import staysplit.hotel_reservation.auth.dto.request.LoginRequest;
import staysplit.hotel_reservation.user.domain.enums.Role;
import staysplit.hotel_reservation.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

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

    private final String testEmail = "test@example.com";
    private final String rawPassword = "12345";
    private final String encodedPassword = "$2a$10$encodedMockPassword";
    private final String token = "mock.jwt.token";

    private UserEntity testUser;

    @BeforeEach
    void setup() {
        testUser = UserEntity.builder()
                .email(testEmail)
                .password(encodedPassword)
                .role(Role.CUSTOMER)
                .build();
    }
    /*
    @Test
    void login_successful_returnsToken() {
        // given
        LoginRequest loginRequest = new LoginRequest(testEmail, rawPassword);

        // when
        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(rawPassword, encodedPassword)).willReturn(true);
        given(jwtTokenProvider.createToken(testEmail, "CUSTOMER")).willReturn(token);

        // then
        UserLoginResponse response = userService.login(loginRequest);

        assertThat(response.jwt()).isEqualTo(token);
    }

    @Test
    void login_userNotFound_throwsException() {
        // given
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", rawPassword);

        given(userRepository.findByEmail(loginRequest.email())).willReturn(Optional.empty());

        // expect
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(AppException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    void login_invalidPassword_throwsException() {
        // given
        LoginRequest loginRequest = new LoginRequest(testEmail, "wrongpassword");

        given(userRepository.findByEmail(testEmail)).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches("wrongpassword", encodedPassword)).willReturn(false);

        // expect
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(AppException.class)
                .hasMessageContaining(ErrorCode.INVALID_PASSWORD.getMessage());
    }*/
}
