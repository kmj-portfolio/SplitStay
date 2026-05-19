package staysplit.hotel_reservation.payment.portone.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.payment.portone.dto.request.PortOneCancelRequest;
import staysplit.hotel_reservation.payment.portone.dto.response.PortOnePaymentResponse;
import staysplit.hotel_reservation.payment.portone.exception.PortOneException;

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
                    throw new PortOneException(ErrorCode.PORT_ONE_GET_PAYMENT_FAILED);
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
                    throw new PortOneException(ErrorCode.PORT_ONE_CANCEL_PAYMENT_FAILED);
                })
                .toBodilessEntity();
    }

}
