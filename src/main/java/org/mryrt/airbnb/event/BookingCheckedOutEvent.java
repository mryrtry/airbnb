package org.mryrt.airbnb.event;

import lombok.Getter;

@Getter
public class BookingCheckedOutEvent {

    private final Long bookingId;

    public BookingCheckedOutEvent(Long bookingId) {
        this.bookingId = bookingId;
    }
}
