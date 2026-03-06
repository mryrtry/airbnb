package org.mryrt.airbnb.resolution.repository.filter;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.mryrt.airbnb.resolution.model.ResolutionStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ResolutionFilter {

    private Long bookingId;
    private ResolutionStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime openedAtAfter;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime openedAtBefore;
}
