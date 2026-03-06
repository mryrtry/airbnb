package org.mryrt.airbnb.listing.service;

import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.auth.service.user.UserService;
import org.mryrt.airbnb.event.EntityEvent;
import org.mryrt.airbnb.exception.ServiceException;
import org.mryrt.airbnb.listing.access.ListingAccessService;
import org.mryrt.airbnb.listing.dto.ListingDto;
import org.mryrt.airbnb.listing.dto.ListingMapper;
import org.mryrt.airbnb.listing.dto.request.CreateListingRequest;
import org.mryrt.airbnb.listing.dto.request.UpdateListingRequest;
import org.mryrt.airbnb.listing.model.Listing;
import org.mryrt.airbnb.listing.model.ListingStatus;
import org.mryrt.airbnb.listing.repository.ListingRepository;
import org.mryrt.airbnb.notification.model.NotificationType;
import org.mryrt.airbnb.notification.service.NotificationService;
import org.mryrt.airbnb.listing.repository.filter.ListingFilter;
import org.mryrt.airbnb.util.pageable.PageableFactory;
import org.mryrt.airbnb.util.pageable.PageableRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

import static org.mryrt.airbnb.event.EventType.CREATED;
import static org.mryrt.airbnb.event.EventType.DELETED;
import static org.mryrt.airbnb.event.EventType.UPDATED;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.ID_MUST_BE_POSITIVE;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.MUST_BE_NOT_NULL;
import static org.mryrt.airbnb.exception.message.GlobalErrorMessage.SOURCE_WITH_ID_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;
    private final ListingMapper mapper;
    private final ListingAccessService listingAccessService;
    private final UserService userService;
    private final PageableFactory pageableFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    private Listing findListing(Long id) {
        if (id == null) throw new ServiceException(MUST_BE_NOT_NULL, "Listing.id");
        if (id <= 0) throw new ServiceException(ID_MUST_BE_POSITIVE, "Listing.id");
        return listingRepository.findById(id)
                .orElseThrow(() -> new ServiceException(SOURCE_WITH_ID_NOT_FOUND, "Listing", id));
    }

    @Override
    @Transactional
    public ListingDto create(CreateListingRequest request) {
        Long ownerId = userService.getAuthenticatedUser().getId();
        Listing listing = Listing.builder()
                .ownerId(ownerId)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(ListingStatus.DRAFT)
                .build();
        ListingDto dto = mapper.toDto(listingRepository.save(listing));
        eventPublisher.publishEvent(new EntityEvent<>(CREATED, dto));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingDto> getAll(ListingFilter filter, PageableRequest pageable) {
        Pageable page = pageableFactory.create(pageable, Listing.class);
        return listingRepository.findWithFilter(filter, page).map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ListingDto get(Long id) {
        return mapper.toDto(findListing(id));
    }

    @Override
    @Transactional
    public ListingDto update(Long id, UpdateListingRequest request) {
        listingAccessService.requireCanModify(id);
        Listing listing = findListing(id);
        ListingStatus previousStatus = listing.getStatus();
        if (request.getTitle() != null) listing.setTitle(request.getTitle());
        if (request.getDescription() != null) listing.setDescription(request.getDescription());
        if (request.getStatus() != null) listing.setStatus(request.getStatus());
        ListingDto dto = mapper.toDto(listingRepository.save(listing));
        if (request.getStatus() != null && listing.getStatus() == ListingStatus.PUBLISHED && previousStatus != ListingStatus.PUBLISHED) {
            notificationService.notifyUser(listing.getOwnerId(), NotificationType.LISTING_PUBLISHED, Map.of("listingId", listing.getId()));
        }
        eventPublisher.publishEvent(new EntityEvent<>(UPDATED, dto));
        return dto;
    }

    @Override
    @Transactional
    public ListingDto delete(Long id) {
        listingAccessService.requireCanModify(id);
        Listing listing = findListing(id);
        listingRepository.delete(listing);
        ListingDto dto = mapper.toDto(listing);
        eventPublisher.publishEvent(new EntityEvent<>(DELETED, dto));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Listing getEntity(Long id) {
        return findListing(id);
    }
}
