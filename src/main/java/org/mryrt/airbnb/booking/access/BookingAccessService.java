package org.mryrt.airbnb.booking.access;

/**
 * Слой проверки доступа к бронированиям.
 * Единственная ответственность: решать, может ли текущий пользователь выполнить действие над ресурсом.
 */
public interface BookingAccessService {

    /**
     * Бросает ServiceException, если текущий пользователь не может просматривать бронирование
     * (не гость, не владелец листинга и не админ с ALL_BOOKING_READ).
     */
    void requireCanRead(Long bookingId);

    /**
     * Бросает ServiceException, если текущий пользователь не может изменять бронирование
     * (не гость, не владелец листинга и не админ с ALL_BOOKING_UPDATE).
     */
    void requireCanUpdate(Long bookingId);

    /**
     * Бросает ServiceException, если текущий пользователь не может выполнить check-in или check-out.
     * Разрешены: гость этого бронирования (получивший одобрение на заселение) или админ (ALL_BOOKING_UPDATE).
     */
    void requireCanCheckInCheckOut(Long bookingId);

    /**
     * Бросает ServiceException, если текущий пользователь не может принять решение по заявке
     * (не владелец листинга и не админ с ALL_BOOKING_UPDATE).
     */
    void requireCanDecide(Long bookingId);

    /**
     * Возвращает id пользователя, по которому нужно ограничить список бронирований (гость или владелец),
     * или null, если у текущего пользователя есть ALL_BOOKING_READ (видит всё).
     */
    Long getScopeUserIdForList();
}
