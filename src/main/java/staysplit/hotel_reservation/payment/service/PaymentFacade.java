package staysplit.hotel_reservation.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.service.CustomerValidator;
import staysplit.hotel_reservation.payment.domain.dto.request.CancelPaymentRequest;
import staysplit.hotel_reservation.payment.domain.dto.response.CancelPaymentResponse;
import staysplit.hotel_reservation.payment.domain.dto.response.PaymentResponse;
import staysplit.hotel_reservation.payment.portone.client.PortOneClient;
import staysplit.hotel_reservation.payment.sse.dto.response.PaymentStatusResponse;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;
import staysplit.hotel_reservation.payment.mapper.PaymentMapper;
import staysplit.hotel_reservation.payment.repository.PaymentRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private final PortOneClient portOneClient;
    private final PaymentTransactionService paymentTransactionService;
    private final CustomerValidator customerValidator;
    private final PaymentRepository paymentRepository;
    private final PaymentValidator paymentValidator;
    private final PaymentMapper paymentMapper;

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPaymentsByCustomer(String email, Pageable pageable) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);

        return paymentRepository.findByCustomerId(customer.getId(), pageable)
                .map(paymentMapper::toPaymentResponse);
    }

    public CancelPaymentResponse cancelPayment(String email, CancelPaymentRequest request) {
        String paymentId = request.paymentId();
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        PaymentEntity paymentEntity = paymentValidator.validatePaymentByPaymentId(paymentId);

        paymentValidator.validatePaymentOwner(paymentEntity, customer);
        paymentValidator.validateCancelable(paymentEntity);

        portOneClient.cancelPayment(paymentId, request.reason());

        try {
           return paymentTransactionService.processCancellation(paymentId);
        } catch (Exception e) {
            log.error("[중요] 포트원 취소는 성공했지만 DB 반영 실패. 수동/재처리 필요. portOnePaymentId={}", paymentId, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "결제 취소는 완료되었지만 내부 상태 반영에 실패했습니다.");
        }
    }

    public PaymentStatusResponse getPaymentStatus(String paymentId, String email) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        PaymentEntity paymentEntity = paymentValidator.validatePaymentByPaymentId(paymentId);
        paymentValidator.validatePaymentOwner(paymentEntity, customer);
        return new PaymentStatusResponse(paymentId, paymentEntity.getStatus());
    }
}