package org.mryrt.airbnb.resolution.service;

import jakarta.validation.Valid;
import org.mryrt.airbnb.resolution.dto.ResolutionDto;
import org.mryrt.airbnb.resolution.dto.request.ComplaintRequest;
import org.mryrt.airbnb.resolution.dto.request.RequestMoneyRequest;
import org.mryrt.airbnb.resolution.dto.request.ResolveEscalationRequest;
import org.mryrt.airbnb.resolution.model.ResolutionWindow;
import org.mryrt.airbnb.resolution.repository.filter.ResolutionFilter;
import org.mryrt.airbnb.util.pageable.PageableRequest;
import org.springframework.data.domain.Page;

public interface ResolutionService {

    ResolutionDto openForBooking(Long bookingId);

    Page<ResolutionDto> getAll(ResolutionFilter filter, PageableRequest pageable);

    ResolutionDto get(Long id);

    ResolutionDto getByBookingId(Long bookingId);

    ResolutionDto requestMoney(Long id, @Valid RequestMoneyRequest request);

    ResolutionDto respondPay(Long id);

    ResolutionDto respondRefuse(Long id);

    ResolutionDto escalate(Long id);

    ResolutionDto recordComplaint(Long id, @Valid ComplaintRequest request);

    ResolutionDto close(Long id);

    ResolutionDto resolveEscalation(Long id, @Valid ResolveEscalationRequest request);

    void closeExpiredWindows();

    ResolutionWindow getEntity(Long id);
}
