package staysplit.hotel_reservation.likeList.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListEntity;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListHotelEntity;
import staysplit.hotel_reservation.likeList.repository.LikeListCustomerRepository;
import staysplit.hotel_reservation.likeList.repository.LikeListHotelRepository;
import staysplit.hotel_reservation.likeList.repository.LikeListRepository;

@Component
@RequiredArgsConstructor
public class LikeListValidator {
    private final LikeListRepository likeListRepository;
    private final LikeListHotelRepository likeListHotelRepository;
    private final LikeListCustomerRepository likeListCustomerRepository;

    public LikeListEntity validateLikeList(Integer listId) {
        return likeListRepository.findById(listId)
                .orElseThrow(() -> new AppException(ErrorCode.LIKE_LIST_NOT_FOUND, ErrorCode.LIKE_LIST_NOT_FOUND.getMessage()));
    }

    public LikeListEntity validateLikeListAndLoadCustomers(Integer listId) {

        return likeListRepository.findByIdWithCustomers(listId)
                .orElseThrow(() -> new AppException(ErrorCode.LIKE_LIST_NOT_FOUND, ErrorCode.LIKE_LIST_NOT_FOUND.getMessage()));
    }

    public LikeListEntity validateLikeListAndLoadCustomersAndHotels(Integer listId) {
        return likeListRepository.findByIdWithCustomersAndHotels(listId)
                .orElseThrow(() -> new AppException(ErrorCode.LIKE_LIST_NOT_FOUND, ErrorCode.LIKE_LIST_NOT_FOUND.getMessage()));
    }

    public boolean isOwner(LikeListEntity likeList, CustomerEntity customer) {
        if (likeList.getOwner().equals(customer)) {
            return true;
        }
        throw new AppException(ErrorCode.UNAUTHORIZED_CUSTOMER, ErrorCode.UNAUTHORIZED_CUSTOMER.getMessage());
    }

    public boolean hasAuthority(LikeListEntity likeList, CustomerEntity customer) {
        if (likeList.getOwner().equals(customer) ||
                likeListCustomerRepository.existsByLikeListAndCustomer(likeList, customer)) {
            return true;
        }
        throw new AppException(ErrorCode.HOTEL_NOT_IN_LIKE_LIST, ErrorCode.HOTEL_NOT_IN_LIKE_LIST.getMessage());
    }

    public LikeListHotelEntity validateLikeListHotel(LikeListEntity likeList, HotelEntity hotel) {
        return likeListHotelRepository.findByLikeListAndHotel(likeList, hotel)
                .orElseThrow(() -> new AppException(ErrorCode.HOTEL_NOT_IN_LIKE_LIST, ErrorCode.HOTEL_NOT_IN_LIKE_LIST.getMessage()));
    }

    public void throwIfHotelInList(LikeListEntity likeList, HotelEntity hotel) {
        if (likeListHotelRepository.existsByLikeListAndHotel(likeList, hotel)) {
            throw new AppException(ErrorCode.HOTEL_ALREADY_IN_LIKE_LIST,
                    ErrorCode.HOTEL_ALREADY_IN_LIKE_LIST.getMessage());
        }
    }
}
