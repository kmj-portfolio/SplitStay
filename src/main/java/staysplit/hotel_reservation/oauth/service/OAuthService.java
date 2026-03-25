package staysplit.hotel_reservation.oauth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staysplit.hotel_reservation.auth.dto.request.LoginRequest;
import staysplit.hotel_reservation.auth.dto.response.LoginTokens;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.common.security.jwt.JwtTokenProvider;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.repository.CustomerRepository;
import staysplit.hotel_reservation.oauth.dto.OauthSignupRequest;
import staysplit.hotel_reservation.reservation.service.UsernameAutocompleteService;
import staysplit.hotel_reservation.user.domain.entity.UserEntity;
import staysplit.hotel_reservation.user.domain.enums.LoginSource;
import staysplit.hotel_reservation.user.domain.enums.Role;
import staysplit.hotel_reservation.user.repository.UserRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class OAuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final UsernameAutocompleteService usernameAutocompleteService;

    // oauth로 회원가입
    public LoginTokens oauthSignup(OauthSignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AppException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (customerRepository.existsByNickname(request.nickname())) {
            throw new AppException(ErrorCode.DUPLICATE_NICKNAME);
        }

        UserEntity user = UserEntity.builder()
                .email(request.email())
                .role(Role.CUSTOMER)
                .loginSource(LoginSource.GOOGLE)
                .socialId(request.socialId())
                .build();

        userRepository.save(user);

        CustomerEntity customer = CustomerEntity.builder()
                .user(user)
                .name(request.name())
                .birthdate(request.birthdate())
                .phoneNumber(request.phoneNumber())
                .nickname(request.nickname())
                .build();

        customerRepository.save(customer);
        usernameAutocompleteService.addUsername(customer.getNickname());

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());
        return new LoginTokens(accessToken, refreshToken, user.getRole().toString());
    }

    public LoginTokens login(String email) {
        String accessToken = jwtTokenProvider.createAccessToken(email);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);
        return new LoginTokens(accessToken, refreshToken, Role.CUSTOMER.toString());
    }
}