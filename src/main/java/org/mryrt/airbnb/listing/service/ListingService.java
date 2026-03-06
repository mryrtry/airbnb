package org.mryrt.airbnb.listing.service;

import jakarta.validation.Valid;
import org.mryrt.airbnb.listing.dto.ListingDto;
import org.mryrt.airbnb.listing.dto.request.CreateListingRequest;
import org.mryrt.airbnb.listing.dto.request.UpdateListingRequest;
import org.mryrt.airbnb.listing.model.Listing;
import org.mryrt.airbnb.listing.repository.filter.ListingFilter;
import org.mryrt.airbnb.util.pageable.PageableRequest;
import org.springframework.data.domain.Page;

public interface ListingService {

    ListingDto create(@Valid CreateListingRequest request);

    Page<ListingDto> getAll(ListingFilter filter, PageableRequest pageable);

    ListingDto get(Long id);

    ListingDto update(Long id, @Valid UpdateListingRequest request);

    ListingDto delete(Long id);

    Listing getEntity(Long id);
}
