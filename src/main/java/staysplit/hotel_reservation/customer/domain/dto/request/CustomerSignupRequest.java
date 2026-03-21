package staysplit.hotel_reservation.customer.domain.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record CustomerSignupRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        String password,

        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(
                regexp = "^01[0-9]\\d{8}$",
                message = "전화번호 형식이 올바르지 않습니다.")
        String phoneNumber,

        @NotBlank(message = "이름을 입력해주세요.")
        String name,

        @NotNull(message = "생년월일을 입력해주세요.")
        @Past(message = "생년월일은 과거여야 합니다.")
        LocalDate birthdate,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 20, message = "닉네임은 2자 이상, 20자 이하여야 합니다.")
        String nickname
) {}

