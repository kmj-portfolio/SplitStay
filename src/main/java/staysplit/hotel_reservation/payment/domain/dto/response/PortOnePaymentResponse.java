package staysplit.hotel_reservation.payment.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePaymentResponse(
        String id,
        String status,
        Amount amount,
        Method method
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(
            long total
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Method(
            String type,
            Card card
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Card(
                String name
        ) {}

        public String cardPublisherName() {
            if (card == null) {
                return null;
            }
            return card.name();
        }
    }

    public String methodType() {
        if (method == null) return null;
        return method.type();
    }

    public String cardPublisherName() {
        if (method == null) return null;
        return method.cardPublisherName();
    }
}
