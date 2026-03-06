package org.mryrt.airbnb.listing.access;

/**
 * Слой проверки доступа к листингам.
 * Единственная ответственность: решать, может ли текущий пользователь изменить/удалить ресурс.
 */
public interface ListingAccessService {

    /**
     * Бросает ServiceException, если текущий пользователь не может изменять или удалять листинг
     * (не владелец и не админ с ALL_LISTING_UPDATE / ALL_LISTING_DELETE).
     */
    void requireCanModify(Long listingId);
}
