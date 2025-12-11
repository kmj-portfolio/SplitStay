package staysplit.hotel_reservation.auth.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordUpdateRequest(
        @Size(min = 8, max = 20)
        @NotBlank(message = "비밀번호를 입력해야 합니다.")
        String password
) {
}
