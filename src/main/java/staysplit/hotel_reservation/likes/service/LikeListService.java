package staysplit.hotel_reservation.likes.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.service.CustomerValidator;
import staysplit.hotel_reservation.likes.domain.dto.response.CreateLikeListResponse;
import staysplit.hotel_reservation.likes.domain.entity.LikeListEntity;
import staysplit.hotel_reservation.likes.repository.LikeListCustomerRepository;
import staysplit.hotel_reservation.likes.repository.LikeListHotelRepository;
import staysplit.hotel_reservation.likes.repository.LikeListRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class LikeListService {
    private final LikeListRepository likeListRepository;

    private final LikeListHotelRepository likeListHotelRepository;
    private final LikeListCustomerRepository sharedUserRepository;

    private final CustomerValidator customerValidator;

    public CreateLikeListResponse createLikeList(String email, String listName) {
        CustomerEntity customer = customerValidator.validateCustomerById(email);
        LikeListEntity likeList = LikeListEntity.builder()
                .owner(customer)
                .listName(listName)
                .build();

        likeListRepository.save(likeList);

        return new CreateLikeListResponse(likeList.getId(), likeList.getOwner().getId(), likeList.getListName());

    }
}
