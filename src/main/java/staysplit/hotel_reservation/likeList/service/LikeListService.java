package staysplit.hotel_reservation.likeList.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.service.CustomerValidator;
import staysplit.hotel_reservation.likeList.dto.response.CreateLikeListResponse;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListEntity;
import staysplit.hotel_reservation.likeList.dto.response.LikeListCollectionResponse;
import staysplit.hotel_reservation.likeList.dto.response.LikeListDetailResponse;
import staysplit.hotel_reservation.likeList.mapper.LikeListMapper;
import staysplit.hotel_reservation.likeList.repository.LikeListRepository;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class LikeListService {
    private final LikeListRepository likeListRepository;
    private final CustomerValidator customerValidator;
    private final LikeListValidator likeListValidator;
    private final LikeListMapper mapper;

    public LikeListDetailResponse getLikeList(String email, Integer listId) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        LikeListEntity likeList = likeListValidator.validateLikeListAndLoadCustomersAndHotels(listId);
        likeListValidator.hasAuthority(likeList, customer);
        return mapper.toDetailResponse(likeList);

    }

    public CreateLikeListResponse createLikeList(String email, String listName) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        LikeListEntity likeList = LikeListEntity.builder()
                .owner(customer)
                .listName(listName)
                .build();

        likeListRepository.save(likeList);

        return new CreateLikeListResponse(likeList.getId(), likeList.getOwner().getId(), likeList.getListName());
    }

    public void deleteLikeList(String email, Integer listId) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        LikeListEntity likeList = likeListValidator.validateLikeList(listId);
        likeListValidator.isOwner(likeList, customer);
        likeListRepository.delete(likeList);
    }

    public void changeListName(String email, Integer listId, String listName) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        LikeListEntity likeList = likeListValidator.validateLikeList(listId);
        likeListValidator.hasAuthority(likeList, customer);
        likeList.changeListName(listName);
    }

    public List<LikeListCollectionResponse> findAllByCustomer(String email) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        List<LikeListEntity> listCollection = likeListRepository.findByCustomerIdWithCustomersAndHotels(customer.getId());
        return mapper.toCollectionResponse(listCollection);
    }
}
