package staysplit.hotel_reservation.dataInitializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staysplit.hotel_reservation.provider.domain.entity.ProviderEntity;
import staysplit.hotel_reservation.provider.repository.ProviderRepository;
import staysplit.hotel_reservation.user.domain.entity.UserEntity;
import staysplit.hotel_reservation.user.domain.enums.LoginSource;
import staysplit.hotel_reservation.user.domain.enums.Role;
import staysplit.hotel_reservation.user.repository.UserRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderCsvImportService {

    private final ProviderRepository providerRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Transactional
    public void importProvidersFromCsv() {
        try {
            ClassPathResource resource = new ClassPathResource("data/providers.csv");

            try (
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
                    );
                    CSVParser csvParser = new CSVParser(
                            reader,
                            CSVFormat.DEFAULT.builder()
                                    .setHeader()
                                    .setSkipHeaderRecord(true)
                                    .build()
                    )
            ) {
                for (CSVRecord record : csvParser) {
                        String email = record.get("email");
                        String password = record.get("password");

                        if (userRepository.existsByEmail(email)) {
                            log.info("이미 존재하는 email={}", email);
                            continue;
                        }

                        UserEntity userEntity = UserEntity.builder()
                                .role(Role.PROVIDER)
                                .email(email)
                                .password(passwordEncoder.encode(password))
                                .loginSource(LoginSource.LOCAL)
                                .build();

                        userRepository.save(userEntity);

                        ProviderEntity providerEntity = ProviderEntity.builder()
                                .user(userEntity)
                                .build();

                        providerRepository.save(providerEntity);

                }
            }
            log.info("Provider CSV import 완료");
        } catch (IOException e) {
            log.error("Provider CSV import 실패 ");
            throw new RuntimeException(e);
        }
    }
}
