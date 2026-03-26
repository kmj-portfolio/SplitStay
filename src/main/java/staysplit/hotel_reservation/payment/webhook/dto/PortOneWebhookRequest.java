package staysplit.hotel_reservation.payment.webhook.dto;

public record PortOneWebhookRequest(
        String type,
        String timestamp,
        Data data
) {
    public record Data(
            String transactionId,
            String paymentId,
            String storeId
    ) { }
}
