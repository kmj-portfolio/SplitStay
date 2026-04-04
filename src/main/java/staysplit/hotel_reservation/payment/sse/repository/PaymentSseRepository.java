package staysplit.hotel_reservation.payment.sse.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class PaymentSseRepository {
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public void save(String paymentId, SseEmitter emitter) {
        emitters.computeIfAbsent(paymentId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    public void deleteByPaymentIdAndEmitter(String paymentId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(paymentId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(paymentId);
            }
        }
    }

    public List<SseEmitter> get(String paymentId) {
        return emitters.getOrDefault(paymentId, Collections.emptyList());
    }

    public List<String> getAllPaymentId() {
        return new ArrayList<>(emitters.keySet());
    }

}
