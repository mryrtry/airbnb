package org.mryrt.airbnb.booking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.booking.dto.BookingDto;
import org.mryrt.airbnb.booking.dto.request.CreateBookingRequest;
import org.mryrt.airbnb.booking.dto.request.DecideBookingRequest;
import org.mryrt.airbnb.booking.repository.filter.BookingFilter;
import org.mryrt.airbnb.booking.service.BookingService;
import org.mryrt.airbnb.util.pageable.PageableRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API заявок на бронирование (бронирований).
 */
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /** Создаёт заявку на бронирование от имени текущего пользователя (гостя). */
    @PostMapping
    @PreAuthorize("hasAuthority('BOOKING_CREATE')")
    public ResponseEntity<BookingDto> create(@Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(request));
    }

    /** Возвращает страницу бронирований с фильтром и сортировкой. */
    @GetMapping
    @PreAuthorize("hasAuthority('BOOKING_READ') or hasAuthority('ALL_BOOKING_READ')")
    public ResponseEntity<Page<BookingDto>> getAll(
            @ModelAttribute BookingFilter filter,
            @ModelAttribute PageableRequest pageable) {
        return ResponseEntity.ok(bookingService.getAll(filter, pageable));
    }

    /** Возвращает бронирование по id. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BOOKING_READ') or hasAuthority('ALL_BOOKING_READ')")
    public ResponseEntity<BookingDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.get(id));
    }

    /** Решение владельца по заявке: одобрить или отклонить. */
    @PutMapping("/{id}/decide")
    @PreAuthorize("hasAuthority('BOOKING_DECIDE') or hasAuthority('ALL_BOOKING_UPDATE')")
    public ResponseEntity<BookingDto> decide(
            @PathVariable Long id,
            @Valid @RequestBody DecideBookingRequest request) {
        return ResponseEntity.ok(bookingService.decide(id, request));
    }

    /** Фиксирует въезд гостя (перевод в статус CHECKED_IN). */
    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAuthority('BOOKING_READ') or hasAuthority('ALL_BOOKING_UPDATE')")
    public ResponseEntity<BookingDto> recordCheckIn(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.recordCheckIn(id));
    }

    /** Фиксирует выезд гостя (перевод в статус CHECKED_OUT). */
    @PutMapping("/{id}/check-out")
    @PreAuthorize("hasAuthority('BOOKING_READ') or hasAuthority('ALL_BOOKING_UPDATE')")
    public ResponseEntity<BookingDto> recordCheckOut(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.recordCheckOut(id));
    }
}
