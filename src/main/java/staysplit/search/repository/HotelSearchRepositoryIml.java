package staysplit.search.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HotelSearchRepositoryIml {
    private final JPAQueryFactory qf;

    
}
