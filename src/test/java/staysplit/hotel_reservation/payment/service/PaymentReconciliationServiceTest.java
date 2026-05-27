package staysplit.hotel_reservation.payment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;
import staysplit.hotel_reservation.payment.portone.client.PortOneClient;
import staysplit.hotel_reservation.payment.portone.dto.response.PortOnePaymentResponse;
import staysplit.hotel_reservation.payment.portone.exception.PortOneException;
import staysplit.hotel_reservation.payment.portone.validator.PortOnePaymentValidator;
import staysplit.hotel_reservation.payment.repository.PaymentRepository;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationParticipantEntity;
import staysplit.hotel_reservation.reservation.service.ReservationValidator;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PortOneClient portOneClient;
    @Mock private ReservationValidator reservationValidator;
    @Mock private PaymentTransactionService paymentTransactionService;
    @Mock private PortOnePaymentValidator portOnePaymentValidator;
    @Mock private PaymentCompensationService paymentCompensationService;

    @InjectMocks private PaymentReconciliationService paymentReconciliationService;

    private static final String PAYMENT_ID = "payment-id-0123456789";

    private PaymentEntity mockPayment() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        given(paymentEntity.getPaymentId()).willReturn(PAYMENT_ID);
        return paymentEntity;
    }

    private PaymentEntity mockPaymentWithReservation(String paymentId, ReservationEntity reservation) {
        ReservationParticipantEntity participant = mock(ReservationParticipantEntity.class);
        given(participant.getReservation()).willReturn(reservation);
        PaymentEntity payment = mock(PaymentEntity.class);
        given(payment.getPaymentId()).willReturn(paymentId);
        given(payment.getReservationParticipant()).willReturn(participant);
        return payment;
    }

    private PortOnePaymentResponse paidResponse() {
        return new PortOnePaymentResponse(
                null,
                PAYMENT_ID,
                "PAID",
                new PortOnePaymentResponse.Amount(1000L),
                null);
    }

    @Nested
    @DisplayName("PG사에서 결제 완료되었지만 서버에서 결제 처리 미완료인 경우")
    class ProcessPaidInPG {

        @Test
        @DisplayName("예약 유효 + 검증 통과 -> processConfirmation 호출")
        void passValidityTest_callsConfirmation() {
            // given
            PaymentEntity payment = mock(PaymentEntity.class);
            ReservationEntity reservation = mock(ReservationEntity.class);
            ReservationParticipantEntity participant = mock(ReservationParticipantEntity.class);
            PortOnePaymentResponse response = new PortOnePaymentResponse(
                    null, PAYMENT_ID, "PAID", new PortOnePaymentResponse.Amount(1000L), null);

            given(payment.getPaymentId()).willReturn(PAYMENT_ID);
            given(payment.getReservationParticipant()).willReturn(participant);
            given(participant.getReservation()).willReturn(reservation);
            given(paymentRepository.findPaymentEntitiesByStatusInAndCreatedAtAfter(any(), any()))
                    .willReturn(List.of(payment));
            given(portOneClient.getPayment(PAYMENT_ID)).willReturn(response);

            // when
            paymentReconciliationService.processPaidInPGbutNotProcessedInServer();

            // then
            then(paymentTransactionService).should().processConfirmation(PAYMENT_ID);
            then(paymentCompensationService).should(never()).cancelPaymentSilently(any(), any());
        }

        @Test
        @DisplayName("PG사에서 조회한 결제 내역이 PAID가 아니면 아무 처리도 하지 않음")
        void whenPGstatusNotPaid_skip() {
            // given
            PaymentEntity paymentEntity = mockPayment();

            PortOnePaymentResponse portOneResponse = new PortOnePaymentResponse(
                    null,
                    PAYMENT_ID,
                    "PENDING",
                    new PortOnePaymentResponse.Amount(1000L),
                    null
            );

            given(paymentRepository.findPaymentEntitiesByStatusInAndCreatedAtAfter(any(), any()))
                    .willReturn(List.of(paymentEntity));
            given(portOneClient.getPayment(PAYMENT_ID)).willReturn(portOneResponse);

            // when
            paymentReconciliationService.processPaidInPGbutNotProcessedInServer();

            // then
            then(paymentTransactionService).should(never()).processConfirmation(PAYMENT_ID);
            then(paymentCompensationService).should(never()).cancelPaymentSilently(any(), any());
        }

        @Test
        @DisplayName("예약이 만료된 경우에는 환불")
        void whenReservationExpired_cancelsPayment() {
            // given
            ReservationEntity reservation = mock(ReservationEntity.class);
            PaymentEntity paymentEntity = mockPaymentWithReservation(PAYMENT_ID, reservation);

            given(paymentRepository.findPaymentEntitiesByStatusInAndCreatedAtAfter(any(), any()))
                    .willReturn(List.of(paymentEntity));
            given(portOneClient.getPayment(PAYMENT_ID)).willReturn(paidResponse());
            willThrow(new AppException(ErrorCode.EXPIRED_RESERVATION))
                    .given(reservationValidator).validateNotExpired(reservation);

            // when
            paymentReconciliationService.processPaidInPGbutNotProcessedInServer();

            // then
            then(paymentCompensationService).should().cancelPaymentSilently(any(), any());
            then(paymentTransactionService).should(never()).processConfirmation(PAYMENT_ID);
        }

        @Test
        @DisplayName("PG 결제 금액과 서버의 결제 금액 불일치인 경우에는 환불")
        void whenAmountMismatch_cancelPayment() {
            // given
            ReservationEntity reservation = mock(ReservationEntity.class);
            PaymentEntity paymentEntity = mockPaymentWithReservation(PAYMENT_ID, reservation);
            PortOnePaymentResponse portOnePaymentResponse = paidResponse();

            given(paymentRepository.findPaymentEntitiesByStatusInAndCreatedAtAfter(any(), any()))
                    .willReturn(List.of(paymentEntity));
            given(portOneClient.getPayment(PAYMENT_ID)).willReturn(portOnePaymentResponse);
            willThrow(new AppException(ErrorCode.PAYMENT_AMOUNT_MISMATCH))
                    .given(portOnePaymentValidator).validateForConfirmation(portOnePaymentResponse, paymentEntity);

            // when
            paymentReconciliationService.processPaidInPGbutNotProcessedInServer();

            // then
            then(paymentCompensationService).should().cancelPaymentSilently(any(), any());
            then(paymentTransactionService).should(never()).processConfirmation(any());
        }

        @Test
        @DisplayName("PortOneException 발생 시 환불 실행 X")
        void whenPortOneException_doesNotCancelPayment() {
            // given
            PaymentEntity paymentEntity = mockPayment();

            given(paymentRepository.findPaymentEntitiesByStatusInAndCreatedAtAfter(any(), any()))
                    .willReturn(List.of(paymentEntity));
            willThrow(new PortOneException(ErrorCode.PORT_ONE_GET_PAYMENT_FAILED))
                    .given(portOneClient).getPayment(PAYMENT_ID);

            // when
            paymentReconciliationService.processPaidInPGbutNotProcessedInServer();

            // then
            then(paymentTransactionService).should(never()).processCancellation(any());
            then(paymentCompensationService).should(never()).cancelPaymentSilently(any(), any());
        }

        @Test
        @DisplayName("여러 건 중에 1건만 실패해도 나머지 건은 계속 처리한다")
        void whenOnePaymentFails_continueRemainingPayments() {
            // given
            String failedPaymentId = "payment-fail";
            String successfulPaymentId = "payment-successful";

            ReservationEntity reservation = mock(ReservationEntity.class);
            PaymentEntity failedPayment = mock(PaymentEntity.class);
            PaymentEntity successfulPayment = mockPaymentWithReservation(successfulPaymentId, reservation);

            given(failedPayment.getPaymentId()).willReturn(failedPaymentId);
            given(paymentRepository.findPaymentEntitiesByStatusInAndCreatedAtAfter(any(), any()))
                    .willReturn(List.of(failedPayment, successfulPayment));
            willThrow(new PortOneException(ErrorCode.PORT_ONE_GET_PAYMENT_FAILED))
                    .given(portOneClient).getPayment(failedPaymentId);
            given(portOneClient.getPayment(successfulPaymentId)).willReturn(paidResponse());

            // when
            paymentReconciliationService.processPaidInPGbutNotProcessedInServer();

            // then
            then(paymentTransactionService).should().processConfirmation(successfulPaymentId);
            then(paymentCompensationService).should(never()).cancelPaymentSilently(any(), any());
        }
    }

    @Nested
    @DisplayName("PG에서는 결제 실패/취소 되었는데 서버에서는 여전히 결제 완료 처리되어있는 경우")
    class processRefundedOrFailedInPGbutPaidInServer {
        @Test
        @DisplayName("PG사에서 조회한 결제 내역의 상태가 REFUNDED이면 서버에서 결제 취소 처리")
        void whenRefunded_callsCancellation() {
            // given
            PaymentEntity paymentEntity = mockPayment();
            given(paymentRepository.findPaymentEntitiesByStatusInAndCreatedAtAfter(any(), any())).willReturn(List.of(paymentEntity));
            PortOnePaymentResponse portOneResponse = new PortOnePaymentResponse(
                    null,
                    PAYMENT_ID,
                    "REFUNDED",
                    new PortOnePaymentResponse.Amount(1000L),
                    null
            );
            given(portOneClient.getPayment(PAYMENT_ID)).willReturn(portOneResponse);

            // when
            paymentReconciliationService.processRefundedOrFailedInPGbutPaidInServer();

            // then
            then(paymentTransactionService).should().processCancellation(PAYMENT_ID);
        }

        @Test
        @DisplayName("PG사에서 조회한 결제 내역의 상태가 FAILED이면 서버에서 결제 취소 처리")
        void whenFailed_callsCancellation() {
            // given
            PaymentEntity paymentEntity = mockPayment();
            given(paymentRepository.findPaymentEntitiesByStatusInAndCreatedAtAfter(any(), any())).willReturn(List.of(paymentEntity));
            PortOnePaymentResponse portOneResponse = new PortOnePaymentResponse(
                    null,
                    PAYMENT_ID,
                    "FAILED",
                    new PortOnePaymentResponse.Amount(1000L),
                    null
            );
            given(portOneClient.getPayment(PAYMENT_ID)).willReturn(portOneResponse);

            // when
            paymentReconciliationService.processRefundedOrFailedInPGbutPaidInServer();

            // then
            then(paymentTransactionService).should().processCancellation(PAYMENT_ID);
        }

        @Test
        @DisplayName("PG사에서 조회한 결제 내역의 상태가 PAID이면 processCancellation 호출 X")
        void whenPaid_skipsProcessCancellation() {
            // given
            PaymentEntity paymentEntity = mockPayment();
            given(paymentRepository.findPaymentEntitiesByStatusInAndCreatedAtAfter(any(), any())).willReturn(List.of(paymentEntity));
            given(portOneClient.getPayment(PAYMENT_ID)).willReturn(paidResponse());

            // when
            paymentReconciliationService.processRefundedOrFailedInPGbutPaidInServer();

            // then
            then(paymentTransactionService).should(never()).processCancellation(PAYMENT_ID);
        }

        @Test
        @DisplayName("한 건에서 예외가 발생해도 다음 건은 계속 처리")
        void whenExceptionOnFirstPayment_continueWithSecond() {
            // given
            String failedPaymentId = "processing-fail";
            String successfulPaymentId = "processing-successful";

            PaymentEntity failedPayment = mock(PaymentEntity.class);
            PaymentEntity successfulPayment = mock(PaymentEntity.class);

            given(failedPayment.getPaymentId()).willReturn(failedPaymentId);
            given(successfulPayment.getPaymentId()).willReturn(successfulPaymentId);

            given(paymentRepository.findPaymentEntitiesByStatusInAndCreatedAtAfter(any(), any()))
                    .willReturn(List.of(failedPayment, successfulPayment));

            willThrow(new PortOneException(ErrorCode.PORT_ONE_GET_PAYMENT_FAILED))
                    .given(portOneClient).getPayment(failedPaymentId);
            PortOnePaymentResponse portOneResponse = new PortOnePaymentResponse(
                    null,
                    PAYMENT_ID,
                    "REFUNDED",
                    new PortOnePaymentResponse.Amount(1000L),
                    null
            );
            given(portOneClient.getPayment(successfulPaymentId)).willReturn(portOneResponse);

            // when
            paymentReconciliationService.processRefundedOrFailedInPGbutPaidInServer();

            // then
            then(paymentTransactionService).should(never()).processCancellation(failedPaymentId);
            then(paymentTransactionService).should().processCancellation(successfulPaymentId);
        }
    }
}
