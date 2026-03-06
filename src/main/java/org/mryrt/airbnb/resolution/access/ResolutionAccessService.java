package org.mryrt.airbnb.resolution.access;

import java.util.List;

/**
 * Слой проверки доступа к окнам разрешения споров.
 * Единственная ответственность: решать, может ли текущий пользователь выполнить действие над ресурсом.
 */
public interface ResolutionAccessService {

    /**
     * Бросает ServiceException, если текущий пользователь не может просматривать окно
     * (не гость бронирования, не владелец листинга и не админ с ALL_RESOLUTION_READ).
     */
    void requireCanRead(Long resolutionId);

    /**
     * Бросает ServiceException, если текущий пользователь не является владельцем листинга
     * по бронированию этого окна (и не админ с ALL_RESOLUTION_UPDATE).
     * Для действий: request-money, escalate, complaint.
     */
    void requireIsOwner(Long resolutionId);

    /**
     * Бросает ServiceException, если текущий пользователь не является гостем бронирования
     * этого окна (и не админ с ALL_RESOLUTION_UPDATE).
     * Для действий: respond-pay, respond-refuse.
     */
    void requireIsGuest(Long resolutionId);

    /**
     * Бросает ServiceException, если текущий пользователь не может закрыть окно
     * (админ с ALL_RESOLUTION_UPDATE или владелец при статусе REFUSED — «закрыть без эскалации»).
     */
    void requireCanClose(Long resolutionId);

    /**
     * Бросает ServiceException, если текущий пользователь не может принять решение по эскалации
     * (только админ с ALL_RESOLUTION_UPDATE: существенное происшествие да/нет).
     */
    void requireCanResolveEscalation(Long resolutionId);

    /**
     * Возвращает список id бронирований, по которым текущий пользователь может видеть резолюции,
     * или null, если у пользователя есть ALL_RESOLUTION_READ (видит всё).
     */
    List<Long> getScopeBookingIdsForList();
}
