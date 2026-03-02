package staysplit.hotel_reservation.likeList.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.service.CustomerValidator;
import staysplit.hotel_reservation.hotel.entity.HotelEntity;
import staysplit.hotel_reservation.hotel.service.HotelValidator;
import staysplit.hotel_reservation.likeList.dto.response.AddHotelResponse;
import staysplit.hotel_reservation.likeList.dto.response.RemoveHotelResponse;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListEntity;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListHotelEntity;
import staysplit.hotel_reservation.likeList.mapper.LikeListMapper;
import staysplit.hotel_reservation.likeList.repository.LikeListHotelRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class LikeListHotelService {

    private final CustomerValidator customerValidator;
    private final LikeListValidator likeListValidator;
    private final HotelValidator hotelValidator;
    private final LikeListMapper mapper;
    private final LikeListHotelRepository likeListHotelRepository;

    public AddHotelResponse addHotelToLikeList(String email, Integer listId, Integer hotelId) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        LikeListEntity likeList = likeListValidator.validateLikeList(listId);
        likeListValidator.hasAuthority(likeList, customer);
        HotelEntity hotel = hotelValidator.validateHotel(hotelId);
        likeListValidator.throwIfHotelInList(likeList, hotel);

        LikeListHotelEntity hotelLike = LikeListHotelEntity.builder()
                .hotel(hotel)
                .likeList(likeList)
                .build();
        likeListHotelRepository.save(hotelLike);
        likeList.getHotels().add(hotelLike);
        return new AddHotelResponse(listId, hotelId);
    }

    public RemoveHotelResponse removeHotelFromListList(String email, Integer listId, Integer hotelId) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        LikeListEntity likeList = likeListValidator.validateLikeList(listId);
        likeListValidator.hasAuthority(likeList, customer);
        HotelEntity hotel = hotelValidator.validateHotel(hotelId);
        LikeListHotelEntity likeListHotel = likeListValidator.validateLikeListHotel(likeList, hotel);
        likeListHotelRepository.delete(likeListHotel);
        return new RemoveHotelResponse(listId, hotelId);
    }
}
