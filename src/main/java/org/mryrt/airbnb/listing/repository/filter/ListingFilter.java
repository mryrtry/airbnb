package org.mryrt.airbnb.listing.repository.filter;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.mryrt.airbnb.listing.model.ListingStatus;

@Getter
@Setter
@Builder
public class ListingFilter {

    private Long ownerId;
    private ListingStatus status;
    private String titleLike;
}
