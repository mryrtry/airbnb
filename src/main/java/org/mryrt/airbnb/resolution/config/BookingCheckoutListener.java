package org.mryrt.airbnb.resolution.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mryrt.airbnb.event.BookingCheckedOutEvent;
import org.mryrt.airbnb.resolution.service.ResolutionService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCheckoutListener {

    private final ResolutionService resolutionService;

    @EventListener
    public void onBookingCheckedOut(BookingCheckedOutEvent event) {
        try {
            resolutionService.openForBooking(event.getBookingId());
            log.debug("Opened resolution window for booking {}", event.getBookingId());
        } catch (Exception e) {
            log.warn("Could not open resolution window for booking {}: {}", event.getBookingId(), e.getMessage());
        }
    }
}
