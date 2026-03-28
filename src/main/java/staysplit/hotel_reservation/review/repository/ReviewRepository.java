package staysplit.hotel_reservation.review.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staysplit.hotel_reservation.review.domain.entity.ReviewEntity;


@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Integer> {

    Page<ReviewEntity> findByHotelId(Integer hotelId, Pageable pageable);

    Page<ReviewEntity> findByCustomer_Id(Integer customerId, Pageable pageable);

    boolean existsByHotel_IdAndCustomer_Id(Integer hotelId, Integer customerId);
}
