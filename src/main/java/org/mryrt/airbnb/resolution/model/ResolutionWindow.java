package org.mryrt.airbnb.resolution.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mryrt.airbnb.model.AuditableEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "resolution_windows")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionWindow extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ResolutionStatus status = ResolutionStatus.OPEN;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "money_requested_at")
    private LocalDateTime moneyRequestedAt;

    @Column(name = "refused_at")
    private LocalDateTime refusedAt;

    @Column(precision = 12, scale = 2)
    private BigDecimal amountRequested;

    @Column(length = 2000)
    private String complaintDescription;
}
