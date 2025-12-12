package staysplit.hotel_reservation.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staysplit.hotel_reservation.auth.dto.request.LoginRequest;
import staysplit.hotel_reservation.auth.dto.request.PasswordUpdateRequest;
import staysplit.hotel_reservation.auth.dto.response.UserLoginResponse;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.common.security.jwt.JwtTokenProvider;
import staysplit.hotel_reservation.user.domain.entity.UserEntity;
import staysplit.hotel_reservation.user.repository.UserRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 일반 로그인
    @Transactional(readOnly = true)
    public UserLoginResponse login(LoginRequest loginRequest) {
        UserEntity user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.getMessage()));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD, ErrorCode.INVALID_PASSWORD.getMessage());
        }

        // 토큰 새성
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        String role = user.getRole().toString();
        return new UserLoginResponse(accessToken, refreshToken, role);
    }

    public String changePassword(PasswordUpdateRequest request, String email) {
        UserEntity user = validateUser(email);
        user.changePassword(passwordEncoder.encode(request.password()));
        return "비밀번호가 변경되었습니다.";
    }

    private UserEntity validateUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND,
                        ErrorCode.USER_NOT_FOUND.getMessage()));
    }
}
