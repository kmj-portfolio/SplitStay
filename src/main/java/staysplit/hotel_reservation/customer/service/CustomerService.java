package staysplit.hotel_reservation.customer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staysplit.hotel_reservation.cart.service.CartService;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.customer.domain.dto.request.CustomerSignupRequest;
import staysplit.hotel_reservation.customer.domain.dto.request.NicknameChangeRequest;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.domain.dto.response.CustomerDetailsResponse;
import staysplit.hotel_reservation.customer.repository.CustomerRepository;
import staysplit.hotel_reservation.reservation.service.UsernameAutocompleteService;
import staysplit.hotel_reservation.user.domain.entity.UserEntity;
import staysplit.hotel_reservation.user.domain.enums.LoginSource;
import staysplit.hotel_reservation.user.domain.enums.Role;
import staysplit.hotel_reservation.user.repository.UserRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final CartService cartService;
    private final UsernameAutocompleteService usernameAutocompleteService;

    public CustomerDetailsResponse signup(CustomerSignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AppException(ErrorCode.DUPLICATE_EMAIL, ErrorCode.DUPLICATE_EMAIL.getMessage());
        }

        if (customerRepository.existsByNickname(request.nickname())) {
            throw new AppException(ErrorCode.DUPLICATE_NICKNAME, ErrorCode.DUPLICATE_NICKNAME.getMessage());
        }

        UserEntity user = UserEntity.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .loginSource(LoginSource.LOCAL)
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
        cartService.createCart(request.email());
        usernameAutocompleteService.addUsername(customer.getNickname());

        return CustomerDetailsResponse.from(customer);
    }

    public CustomerDetailsResponse getMyProfile(String email) {
        CustomerEntity customer = validateCustomer(email);
        return CustomerDetailsResponse.from(customer);
    }

    public CustomerDetailsResponse findCustomerById(Integer id) {
        CustomerEntity customer = validateCustomer(id);
        return CustomerDetailsResponse.from(customer);
    }

    public CustomerDetailsResponse changeNickname(NicknameChangeRequest request, String email) {
        CustomerEntity customer = validateCustomer(email);
        if (customerRepository.existsByNickname(request.getNickname())) {
            throw new AppException(ErrorCode.DUPLICATE_NICKNAME, ErrorCode.DUPLICATE_NICKNAME.getMessage());
        }
        usernameAutocompleteService.removeUsername(customer.getNickname());
        customer.setNickname(request.getNickname());
        usernameAutocompleteService.addUsername(request.getNickname());
        return CustomerDetailsResponse.from(customer);
    }

    public void delete(String email) {
        CustomerEntity customer = validateCustomer(email);
        usernameAutocompleteService.removeUsername(customer.getNickname());
        cartService.deleteCart(email);
        customerRepository.delete(customer);
    }

    private CustomerEntity validateCustomer(Integer id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.getMessage()));
    }

    private CustomerEntity validateCustomer(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.getMessage()));

        return customerRepository.findByUser(user)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.getMessage()));

    }

}
