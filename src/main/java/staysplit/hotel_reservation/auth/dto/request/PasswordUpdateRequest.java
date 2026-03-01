package staysplit.hotel_reservation.auth.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordUpdateRequest(
        String currentPassword,

        @Size(min = 8, max = 20)
        @NotBlank(message = "새로운 비밀번호를 입력해 주세요.")
        String newPassword
) {
}
