package staysplit.hotel_reservation.common.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import staysplit.hotel_reservation.common.entity.Response;
import staysplit.hotel_reservation.common.exception.ErrorResponse;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        response.setStatus(HttpStatus.FORBIDDEN.value()); // 403
        response.setContentType("application/json;charset=UTF-8");

        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.toString(),
                "접근 권한이 업습니다."
        );

        Response<ErrorResponse> body = Response.error(error);

        String json = objectMapper.writeValueAsString(body);
        response.getWriter().write(json);
    }
}
