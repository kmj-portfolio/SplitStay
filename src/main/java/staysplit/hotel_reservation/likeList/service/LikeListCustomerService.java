package staysplit.hotel_reservation.likeList.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.customer.service.CustomerValidator;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListCustomer;
import staysplit.hotel_reservation.likeList.domain.entity.LikeListEntity;
import staysplit.hotel_reservation.likeList.repository.LikeListCustomerRepository;
import staysplit.hotel_reservation.likeList.repository.LikeListRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class LikeListCustomerService {

    private final LikeListRepository likeListRepository;

    private final LikeListCustomerRepository likeListCustomerRepository;

    private final CustomerValidator customerValidator;
    private final LikeListValidator likeListValidator;
    public void addCustomerToLikeList(String email, Integer listId, String invitedEmail) {
        throwIfInvitedEmailIsNotTheSameAsOwners(email, invitedEmail);
        CustomerEntity owner = customerValidator.validateCustomerByEmail(email);
        CustomerEntity invitedCustomer = customerValidator.validateCustomerByEmail(invitedEmail);
        LikeListEntity likeList = likeListValidator.validateLikeList(listId);
        likeListValidator.isOwner(likeList, owner);

        LikeListCustomer participant = LikeListCustomer.builder()
                .likeList(likeList)
                .customer(invitedCustomer)
                .build();

        likeListCustomerRepository.save(participant);
        likeList.getParticipants().add(participant);
    }

    public void leaveLikeList(String email, Integer listId) {
        CustomerEntity customer = customerValidator.validateCustomerByEmail(email);
        LikeListEntity likeList = likeListValidator.validateLikeListAndLoadCustomers(listId);
        if (likeList.getOwner().equals(customer)) {
            likeListRepository.delete(likeList);
        } else if (!likeList.getParticipants().contains(customer)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_CUSTOMER, ErrorCode.UNAUTHORIZED_CUSTOMER.getMessage());
        } else {
            likeList.getParticipants().remove(customer);
        }
    }

    public void removeParticipantFromLikeList(String email, Integer listId, String participantEmail) {
        CustomerEntity owner = customerValidator.validateCustomerByEmail(email);
        CustomerEntity participant = customerValidator.validateCustomerByEmail(participantEmail);
        LikeListEntity likeList = likeListValidator.validateLikeListAndLoadCustomers(listId);
        likeListValidator.isOwner(likeList, owner);
        if (likeList.getParticipants().contains(participant)) {
            throw new AppException(ErrorCode.LIKE_LIST_PARTICIPANT_NOT_FOUND, ErrorCode.LIKE_LIST_PARTICIPANT_NOT_FOUND.getMessage());
        }
        likeList.getParticipants().remove(participant);
    }

    private void throwIfInvitedEmailIsNotTheSameAsOwners(String email, String invitedEmail) {
        if (email.equals(invitedEmail)) {
            throw new AppException(ErrorCode.SAME_EMAIL_AS_OWNER, ErrorCode.SAME_EMAIL_AS_OWNER.getMessage());
        }
    }
}
