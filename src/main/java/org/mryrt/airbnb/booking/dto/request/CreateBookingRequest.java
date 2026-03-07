package org.mryrt.airbnb.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotNull(message = "Booking.listingId не может быть пустым")
    private Long listingId;

    @NotNull(message = "Дата въезда обязательна")
    private LocalDate checkInDate;

    @NotNull(message = "Дата выезда обязательна")
    private LocalDate checkOutDate;
}
