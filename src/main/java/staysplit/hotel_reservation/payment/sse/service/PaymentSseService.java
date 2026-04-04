package staysplit.hotel_reservation.payment.sse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.service.CustomerValidator;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;
import staysplit.hotel_reservation.payment.domain.enums.PaymentStatus;
import staysplit.hotel_reservation.payment.service.PaymentValidator;
import staysplit.hotel_reservation.payment.sse.dto.response.PaymentStatusEvent;
import staysplit.hotel_reservation.payment.sse.repository.PaymentSseRepository;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSseService {
    private static final long DEFAULT_TIMEOUT = 60 * 1000 * 5; // 5분

    private final PaymentSseRepository paymentSseRepository;
    private final PaymentValidator paymentValidator;
    private final CustomerValidator customerValidator;

    public SseEmitter connect(String paymentId, String email) {
        CustomerEntity customerEntity = customerValidator.validateCustomerByEmail(email);
        PaymentEntity paymentEntity = paymentValidator.validatePaymentByPaymentId(paymentId);
        paymentValidator.validatePaymentOwner(paymentEntity, customerEntity);

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        paymentSseRepository.save(paymentId, emitter);

        log.info("[SSE 연결] paymentId={}", paymentId);

        emitter.onCompletion(() -> paymentSseRepository.deleteByPaymentIdAndEmitter(paymentId, emitter));
        emitter.onTimeout(() -> {
            log.info("[SSE timeout] paymentId={}", paymentId);
            emitter.complete();
        });
        emitter.onError((ex) -> {
            log.warn("[SSE error] paymentId={}", paymentId);
            emitter.complete();
        });

        sendConnectedEvent(paymentId, emitter);
        return emitter;
    }

    public void sendPaymentCompleted(String paymentId) {
        log.info("[SSE 결제 완료] paymentId={}", paymentId);
        PaymentStatusEvent payload = PaymentStatusEvent.completed(paymentId);
        sendEvent(paymentId, "payment-complete", payload);
    }

    public void sendPaymentFailed(String paymentId) {
        log.info("[SSE 결제 실패] paymentId={}", paymentId);
        PaymentStatusEvent payload = PaymentStatusEvent.failed(paymentId);
        sendEvent(paymentId, "payment-failed", payload);
    }

    public void sendPaymentCancelled(String paymentId) {
        log.info("[SSE 결제 취소] paymentId={}", paymentId);
        PaymentStatusEvent payload = PaymentStatusEvent.cancelled(paymentId);
        sendEvent(paymentId, "payment-cancelled", payload);
    }

    public void sendHeartBeatToAll() {
        List<String> paymentIdList = paymentSseRepository.getAllPaymentId();
        paymentIdList.forEach(this::sendHeartbeat);
        log.debug("[SSE heartbeat] 활성 연결 수={}", paymentIdList.size());
    }

    private void sendHeartbeat(String paymentId) {
        List<SseEmitter> sseEmitters = paymentSseRepository.get(paymentId);
        if (sseEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter sseEmitter : sseEmitters) {
            try {
                sseEmitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .comment("keep-alive"));
            } catch (IOException e) {
                log.warn("[SSE heartbeat 실패] paymentId={}", paymentId);
                paymentSseRepository.deleteByPaymentIdAndEmitter(paymentId, sseEmitter);
            }
        }
    }

    private void sendConnectedEvent(String paymentId, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(new PaymentStatusEvent(paymentId, PaymentStatus.READY, "SSE 연결이 완료되었습니다.")));
        } catch (IOException e) {
            log.warn("[SSE connected 전송 실패] paymentId={}", paymentId);
            emitter.complete();
        }
    }

    private void sendEvent(String paymentId, String eventName, PaymentStatusEvent paymentStatusEvent) {
        List<SseEmitter> sseEmitters = paymentSseRepository.get(paymentId);

        for (SseEmitter sseEmitter : sseEmitters) {
            try {
                sseEmitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(paymentStatusEvent));
                sseEmitter.complete();
            } catch (IOException e) {
                log.warn("[SSE send 실패] paymentId={}, eventName={}", paymentId, eventName);
                paymentSseRepository.deleteByPaymentIdAndEmitter(paymentId, sseEmitter);
            }
        }
    }

}
