package org.mryrt.airbnb.notification.message;

import org.mryrt.airbnb.notification.model.NotificationType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class DefaultNotificationMessageProvider implements NotificationMessageProvider {

    @Override
    public NotificationContent getContent(NotificationType type, Map<String, Object> context) {
        return switch (type) {
            case LISTING_PUBLISHED -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.LISTING_PUBLISHED_TITLE)
                    .body(String.format(NotificationMessageTemplates.LISTING_PUBLISHED_BODY, context.get("listingId"))).build();
            case BOOKING_APPLIED -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.BOOKING_APPLIED_TITLE)
                    .body(String.format(NotificationMessageTemplates.BOOKING_APPLIED_BODY,
                            context.get("listingId"), context.get("bookingId"))).build();
            case BOOKING_APPROVED -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.BOOKING_APPROVED_TITLE)
                    .body(String.format(NotificationMessageTemplates.BOOKING_APPROVED_BODY, context.get("bookingId"))).build();
            case BOOKING_REJECTED -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.BOOKING_REJECTED_TITLE)
                    .body(String.format(NotificationMessageTemplates.BOOKING_REJECTED_BODY, context.get("bookingId"))).build();
            case BOOKING_CHECKED_IN -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.BOOKING_CHECKED_IN_TITLE)
                    .body(String.format(NotificationMessageTemplates.BOOKING_CHECKED_IN_BODY, context.get("bookingId"))).build();
            case BOOKING_CHECKED_OUT -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.BOOKING_CHECKED_OUT_TITLE)
                    .body(String.format(NotificationMessageTemplates.BOOKING_CHECKED_OUT_BODY, context.get("bookingId"), NotificationMessageTemplates.RESOLUTION_WINDOW_DAYS)).build();
            case RESOLUTION_WINDOW_OPENED -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.RESOLUTION_WINDOW_OPENED_TITLE)
                    .body(String.format(NotificationMessageTemplates.RESOLUTION_WINDOW_OPENED_BODY, context.get("bookingId"), NotificationMessageTemplates.RESOLUTION_WINDOW_DAYS)).build();
            case RESOLUTION_MONEY_REQUESTED -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.RESOLUTION_MONEY_REQUESTED_TITLE)
                    .body(String.format(NotificationMessageTemplates.RESOLUTION_MONEY_REQUESTED_BODY, context.get("bookingId"), formatAmount(context.get("amount")))).build();
            case RESOLUTION_PAYMENT_RECEIVED -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.RESOLUTION_PAYMENT_RECEIVED_TITLE)
                    .body(String.format(NotificationMessageTemplates.RESOLUTION_PAYMENT_RECEIVED_BODY, context.get("bookingId"))).build();
            case RESOLUTION_GUEST_REFUSED -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.RESOLUTION_GUEST_REFUSED_TITLE)
                    .body(String.format(NotificationMessageTemplates.RESOLUTION_GUEST_REFUSED_BODY, context.get("bookingId"))).build();
            case RESOLUTION_COMPLAINT_RECORDED -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.RESOLUTION_COMPLAINT_RECORDED_TITLE)
                    .body(String.format(NotificationMessageTemplates.RESOLUTION_COMPLAINT_RECORDED_BODY, context.get("bookingId"))).build();
            case RESOLUTION_WINDOW_CLOSED -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.RESOLUTION_WINDOW_CLOSED_TITLE)
                    .body(String.format(NotificationMessageTemplates.RESOLUTION_WINDOW_CLOSED_BODY, context.get("bookingId"))).build();
            case RESOLUTION_WINDOW_CLOSED_AUTO -> NotificationContent.builder()
                    .title(String.format(NotificationMessageTemplates.RESOLUTION_WINDOW_CLOSED_AUTO_TITLE, NotificationMessageTemplates.RESOLUTION_WINDOW_DAYS))
                    .body(String.format(NotificationMessageTemplates.RESOLUTION_WINDOW_CLOSED_AUTO_BODY, context.get("bookingId"), NotificationMessageTemplates.RESOLUTION_WINDOW_DAYS)).build();
            case RESOLUTION_ESCALATED -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.RESOLUTION_ESCALATED_TITLE)
                    .body(String.format(NotificationMessageTemplates.RESOLUTION_ESCALATED_BODY, context.get("bookingId"))).build();
            case RESOLUTION_MANDATORY_PAYMENT_ASSIGNED -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.RESOLUTION_MANDATORY_PAYMENT_ASSIGNED_TITLE)
                    .body(String.format(NotificationMessageTemplates.RESOLUTION_MANDATORY_PAYMENT_ASSIGNED_BODY, context.get("bookingId"), formatAmount(context.get("amount")))).build();
            case RESOLUTION_RESOLVED_WITHOUT_PAYMENT -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.RESOLUTION_RESOLVED_WITHOUT_PAYMENT_TITLE)
                    .body(String.format(NotificationMessageTemplates.RESOLUTION_RESOLVED_WITHOUT_PAYMENT_BODY, context.get("bookingId"))).build();
            case RESOLUTION_WINDOW_CLOSED_BY_OWNER -> NotificationContent.builder()
                    .title(NotificationMessageTemplates.RESOLUTION_WINDOW_CLOSED_BY_OWNER_TITLE)
                    .body(String.format(NotificationMessageTemplates.RESOLUTION_WINDOW_CLOSED_BY_OWNER_BODY, context.get("bookingId"))).build();
        };
    }

    private static String formatAmount(Object amount) {
        if (amount == null) return "—";
        if (amount instanceof BigDecimal bd) return bd.toPlainString();
        return String.valueOf(amount);
    }
}
