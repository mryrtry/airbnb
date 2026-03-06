package org.mryrt.airbnb.resolution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mryrt.airbnb.resolution.model.ResolutionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionDto {

    private Long id;
    private Long bookingId;
    private ResolutionStatus status;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private LocalDateTime moneyRequestedAt;
    private LocalDateTime refusedAt;
    private BigDecimal amountRequested;
    private String complaintDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
