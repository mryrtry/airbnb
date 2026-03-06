package org.mryrt.airbnb.listing.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.listing.dto.ListingDto;
import org.mryrt.airbnb.listing.dto.request.CreateListingRequest;
import org.mryrt.airbnb.listing.dto.request.UpdateListingRequest;
import org.mryrt.airbnb.listing.repository.filter.ListingFilter;
import org.mryrt.airbnb.listing.service.ListingService;
import org.mryrt.airbnb.util.pageable.PageableRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API объявлений о сдаче жилья (листингов).
 */
@RestController
@RequestMapping("/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    /** Создаёт объявление от имени текущего пользователя (владельца). */
    @PostMapping
    @PreAuthorize("hasAuthority('LISTING_CREATE')")
    public ResponseEntity<ListingDto> create(@Valid @RequestBody CreateListingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(listingService.create(request));
    }

    /** Возвращает страницу объявлений с фильтром и сортировкой. */
    @GetMapping
    @PreAuthorize("hasAuthority('LISTING_READ') or hasAuthority('ALL_LISTING_READ')")
    public ResponseEntity<Page<ListingDto>> getAll(
            @ModelAttribute ListingFilter filter,
            @ModelAttribute PageableRequest pageable) {
        return ResponseEntity.ok(listingService.getAll(filter, pageable));
    }

    /** Возвращает объявление по id. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LISTING_READ') or hasAuthority('ALL_LISTING_READ')")
    public ResponseEntity<ListingDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.get(id));
    }

    /** Обновляет объявление (заголовок, описание, статус). */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LISTING_UPDATE') or hasAuthority('ALL_LISTING_UPDATE')")
    public ResponseEntity<ListingDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListingRequest request) {
        return ResponseEntity.ok(listingService.update(id, request));
    }

    /** Удаляет объявление. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LISTING_DELETE') or hasAuthority('ALL_LISTING_DELETE')")
    public ResponseEntity<ListingDto> delete(@PathVariable Long id) {
        return ResponseEntity.ok(listingService.delete(id));
    }
}
