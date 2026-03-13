package staysplit.hotel_reservation.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.payment.domain.dto.request.PortOneCancelRequest;
import staysplit.hotel_reservation.payment.domain.dto.response.PortOnePaymentResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortOneClient {

    private final RestClient portOneRestClient;

    public PortOnePaymentResponse getPayment(String portOnePaymentId) {
        return portOneRestClient.get()
                .uri("/payments/{paymentId}", portOnePaymentId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.error("포트원 결제 조회 API 에러: {}", res.getStatusCode());
                    throw new AppException(ErrorCode.INVALID_PAYMENT, "포트원 결제 내역을 불러오는 데 실패했습니다.");
                })
                .body(PortOnePaymentResponse.class);
    }

    public void cancelPayment(String portOnePaymentId, String reason) {
        portOneRestClient.post()
                .uri("/payments/{paymentId}/cancel", portOnePaymentId)
                .body(new PortOneCancelRequest(reason))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    log.error("포트원 결제 취소 API 에러: {}", res.getStatusCode());
                    // 상황에 맞는 적절한 커스텀 예외를 던집니다.
                    throw new AppException(ErrorCode.INVALID_PAYMENT, "포트원 결제 취소에 실패했습니다.");
                })
                .toBodilessEntity();
    }

}
