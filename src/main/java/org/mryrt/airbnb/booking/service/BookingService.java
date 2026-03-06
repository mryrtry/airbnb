package org.mryrt.airbnb.booking.service;

import jakarta.validation.Valid;
import org.mryrt.airbnb.booking.dto.BookingDto;
import org.mryrt.airbnb.booking.dto.request.CreateBookingRequest;
import org.mryrt.airbnb.booking.dto.request.DecideBookingRequest;
import org.mryrt.airbnb.booking.model.Booking;
import org.mryrt.airbnb.booking.repository.filter.BookingFilter;
import org.mryrt.airbnb.util.pageable.PageableRequest;
import org.springframework.data.domain.Page;

public interface BookingService {

    BookingDto create(@Valid CreateBookingRequest request);

    Page<BookingDto> getAll(BookingFilter filter, PageableRequest pageable);

    BookingDto get(Long id);

    BookingDto decide(Long id, @Valid DecideBookingRequest request);

    BookingDto recordCheckIn(Long id);

    BookingDto recordCheckOut(Long id);

    Booking getEntity(Long id);
}
