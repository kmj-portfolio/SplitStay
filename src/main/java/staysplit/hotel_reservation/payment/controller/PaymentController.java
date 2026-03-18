package staysplit.hotel_reservation.payment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import staysplit.hotel_reservation.payment.domain.dto.request.CancelPaymentRequest;
import staysplit.hotel_reservation.payment.domain.dto.request.CreatePaymentRequest;
import staysplit.hotel_reservation.payment.domain.dto.request.VerifyPaymentRequest;
import staysplit.hotel_reservation.payment.domain.dto.response.PaymentResponse;
import staysplit.hotel_reservation.payment.service.PaymentFacade;
import staysplit.hotel_reservation.common.entity.Response;
import staysplit.hotel_reservation.payment.service.PaymentTransactionService;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentFacade paymentFacade;
    private final PaymentTransactionService paymentTransactionService;

    @PostMapping
    public Response<PaymentResponse> createPayment(Authentication authentication, @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentTransactionService.createPayment(authentication.getName(), request);
        return Response.success(response);
    }

    @PostMapping("/verifications")
    public Response<PaymentResponse> verifyPGPayment(Authentication authentication, @RequestBody VerifyPaymentRequest request) {
        PaymentResponse response = paymentFacade.verifyPayment(authentication.getName(), request);
        log.info("[결제 검증 완료]");
        return Response.success(response);
    }

    @PostMapping("/cancel")
    public Response<String> cancelPayment(Authentication authentication, @RequestBody CancelPaymentRequest request) {
        paymentFacade.cancelPayment(authentication.getName(), request);
        return Response.success("결제가 취소되었습니다.");
    }


    @GetMapping("/my")
    public Response<Page<PaymentResponse>> getPaymentsByCustomer(Authentication authentication, Pageable pageable) {
        Page<PaymentResponse> responses = paymentFacade.getPaymentsByCustomer(authentication.getName(), pageable);
        return Response.success(responses);
    }

}