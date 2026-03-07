package org.mryrt.airbnb.notification.message;

public final class NotificationMessageTemplates {

    public static final int RESOLUTION_WINDOW_DAYS = 14;

    public static final String LISTING_PUBLISHED_TITLE = "Объявление опубликовано";
    public static final String LISTING_PUBLISHED_BODY = "Ваше объявление #%s успешно опубликовано и доступно для бронирований.";

    public static final String BOOKING_APPLIED_TITLE = "Новая заявка на бронирование";
    public static final String BOOKING_APPLIED_BODY = "По объявлению #%s новая заявка #%s. Одобрите или отклоните в разделе бронирований.";

    public static final String BOOKING_APPROVED_TITLE = "Заявка одобрена";
    public static final String BOOKING_APPROVED_BODY = "Ваша заявка #%s одобрена. Можете заехать в жильё.";

    public static final String BOOKING_REJECTED_TITLE = "Заявка отклонена";
    public static final String BOOKING_REJECTED_BODY = "К сожалению, заявка #%s отклонена владельцем.";

    public static final String BOOKING_REJECTED_ANOTHER_APPROVED_TITLE = "Заявка отклонена — одобрена другая";
    public static final String BOOKING_REJECTED_ANOTHER_APPROVED_BODY = "Ваша заявка #%s по объявлению #%s отклонена: на эти даты одобрена заявка другого гостя. На одну дату может заехать только один гость.";

    public static final String BOOKING_CHECKED_IN_TITLE = "Гость въехал";
    public static final String BOOKING_CHECKED_IN_BODY = "Гость заехал по бронированию #%s.";

    public static final String BOOKING_CHECKED_OUT_TITLE = "Гость выехал";
    public static final String BOOKING_CHECKED_OUT_BODY = "Гость выехал по бронированию #%s. Открыто окно разрешения споров на %d дней.";

    public static final String RESOLUTION_WINDOW_OPENED_TITLE = "Открыто окно разрешения споров";
    public static final String RESOLUTION_WINDOW_OPENED_BODY = "По бронированию #%s открыто окно. Вы можете запросить компенсацию или подать жалобу в течение %d дней.";

    public static final String RESOLUTION_MONEY_REQUESTED_TITLE = "Запрос на компенсацию";
    public static final String RESOLUTION_MONEY_REQUESTED_BODY = "Владелец запросил компенсацию по бронированию #%s. Сумма: %s. Оплатите или откажите в окне разрешения споров.";

    public static final String RESOLUTION_PAYMENT_RECEIVED_TITLE = "Оплата получена";
    public static final String RESOLUTION_PAYMENT_RECEIVED_BODY = "Гость оплатил компенсацию по бронированию #%s. Конфликт урегулирован.";

    public static final String RESOLUTION_GUEST_REFUSED_TITLE = "Гость отказал в оплате";
    public static final String RESOLUTION_GUEST_REFUSED_BODY = "Гость отказался оплатить компенсацию по бронированию #%s. Вы можете попросить Airbnb вмешаться.";

    public static final String RESOLUTION_COMPLAINT_RECORDED_TITLE = "Жалоба зафиксирована";
    public static final String RESOLUTION_COMPLAINT_RECORDED_BODY = "Ваша жалоба по бронированию #%s зафиксирована.";

    public static final String RESOLUTION_WINDOW_CLOSED_TITLE = "Окно разрешения споров закрыто";
    public static final String RESOLUTION_WINDOW_CLOSED_BODY = "Окно по бронированию #%s закрыто.";

    public static final String RESOLUTION_WINDOW_CLOSED_AUTO_TITLE = "Окно разрешения споров закрыто (%d дней)";
    public static final String RESOLUTION_WINDOW_CLOSED_AUTO_BODY = "Окно по бронированию #%s автоматически закрыто: в течение %d дней не было действий.";

    public static final String RESOLUTION_ESCALATED_TITLE = "Запрос на вмешательство";
    public static final String RESOLUTION_ESCALATED_BODY = "Владелец запросил вмешательство по бронированию #%s.";

    public static final String RESOLUTION_MANDATORY_PAYMENT_ASSIGNED_TITLE = "Назначена обязательная выплата";
    public static final String RESOLUTION_MANDATORY_PAYMENT_ASSIGNED_BODY = "По бронированию #%s назначена обязательная компенсация. Сумма: %s. Оплатите в окне разрешения споров.";

    public static final String RESOLUTION_RESOLVED_WITHOUT_PAYMENT_TITLE = "Конфликт урегулирован без взыскания";
    public static final String RESOLUTION_RESOLVED_WITHOUT_PAYMENT_BODY = "По бронированию #%s конфликт урегулирован. Обязательная выплата не назначена.";

    public static final String RESOLUTION_WINDOW_CLOSED_BY_OWNER_TITLE = "Окно разрешения споров закрыто";
    public static final String RESOLUTION_WINDOW_CLOSED_BY_OWNER_BODY = "Окно по бронированию #%s закрыто владельцем без эскалации.";

    private NotificationMessageTemplates() {
    }
}
