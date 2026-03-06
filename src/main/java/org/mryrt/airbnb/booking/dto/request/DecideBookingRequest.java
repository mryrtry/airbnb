package org.mryrt.airbnb.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mryrt.airbnb.booking.model.BookingStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DecideBookingRequest {

    @NotNull(message = "Booking.status не может быть пустым")
    private BookingStatus status;
}
