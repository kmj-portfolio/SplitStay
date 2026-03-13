package staysplit.hotel_reservation.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class PortOneConfig {

    @Value("${portone.api.secret}")
    private String apiSecret;

    @Bean
    public RestClient portOneRestClient() {
        // 타임아웃 설정
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        // RestClient 기본 설정
        return RestClient.builder()
                .baseUrl("https://api.portone.io")
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "PortOne " + apiSecret) //PortOne 뒤에 공백 하나 필요
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
