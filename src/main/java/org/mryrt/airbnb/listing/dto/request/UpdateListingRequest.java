package org.mryrt.airbnb.listing.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mryrt.airbnb.listing.model.ListingStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateListingRequest {

    @Size(max = 500)
    private String title;

    @Size(max = 2000)
    private String description;

    private ListingStatus status;
}
