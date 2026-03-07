package org.mryrt.airbnb.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mryrt.airbnb.booking.model.BookingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDto {

    private Long id;
    private Long listingId;
    private Long guestId;
    private BookingStatus status;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private LocalDateTime appliedAt;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
