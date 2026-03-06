package org.mryrt.airbnb.resolution.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.resolution.dto.ResolutionDto;
import org.mryrt.airbnb.resolution.dto.request.ComplaintRequest;
import org.mryrt.airbnb.resolution.dto.request.RequestMoneyRequest;
import org.mryrt.airbnb.resolution.dto.request.ResolveEscalationRequest;
import org.mryrt.airbnb.resolution.repository.filter.ResolutionFilter;
import org.mryrt.airbnb.resolution.service.ResolutionService;
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
 * API окна разрешения споров после выезда (запрос компенсации, жалоба, эскалация).
 */
@RestController
@RequestMapping("/resolutions")
@RequiredArgsConstructor
public class ResolutionController {

    private final ResolutionService resolutionService;

    /** Открывает окно разрешения споров для бронирования (после выезда). */
    @PostMapping("/open/{bookingId}")
    @PreAuthorize("hasAuthority('RESOLUTION_READ') or hasAuthority('ALL_RESOLUTION_UPDATE')")
    public ResponseEntity<ResolutionDto> openForBooking(@PathVariable Long bookingId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resolutionService.openForBooking(bookingId));
    }

    /** Возвращает страницу окон разрешения с фильтром и сортировкой. */
    @GetMapping
    @PreAuthorize("hasAuthority('RESOLUTION_READ') or hasAuthority('ALL_RESOLUTION_READ')")
    public ResponseEntity<Page<ResolutionDto>> getAll(
            @ModelAttribute ResolutionFilter filter,
            @ModelAttribute PageableRequest pageable) {
        return ResponseEntity.ok(resolutionService.getAll(filter, pageable));
    }

    /** Возвращает окно разрешения по id. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RESOLUTION_READ') or hasAuthority('ALL_RESOLUTION_READ')")
    public ResponseEntity<ResolutionDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(resolutionService.get(id));
    }

    /** Возвращает окно разрешения по id бронирования. */
    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAuthority('RESOLUTION_READ') or hasAuthority('ALL_RESOLUTION_READ')")
    public ResponseEntity<ResolutionDto> getByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(resolutionService.getByBookingId(bookingId));
    }

    /** Запрос владельца на материальную компенсацию с гостя. */
    @PutMapping("/{id}/request-money")
    @PreAuthorize("hasAuthority('RESOLUTION_REQUEST_MONEY') or hasAuthority('ALL_RESOLUTION_UPDATE')")
    public ResponseEntity<ResolutionDto> requestMoney(
            @PathVariable Long id,
            @Valid @RequestBody RequestMoneyRequest request) {
        return ResponseEntity.ok(resolutionService.requestMoney(id, request));
    }

    /** Ответ гостя: согласие на оплату (конфликт урегулирован). */
    @PutMapping("/{id}/respond-pay")
    @PreAuthorize("hasAuthority('RESOLUTION_RESPOND') or hasAuthority('ALL_RESOLUTION_UPDATE')")
    public ResponseEntity<ResolutionDto> respondPay(@PathVariable Long id) {
        return ResponseEntity.ok(resolutionService.respondPay(id));
    }

    /** Ответ гостя: отказ от оплаты. */
    @PutMapping("/{id}/respond-refuse")
    @PreAuthorize("hasAuthority('RESOLUTION_RESPOND') or hasAuthority('ALL_RESOLUTION_UPDATE')")
    public ResponseEntity<ResolutionDto> respondRefuse(@PathVariable Long id) {
        return ResponseEntity.ok(resolutionService.respondRefuse(id));
    }

    /** Владелец просит вмешательства (эскалация после отказа гостя). */
    @PutMapping("/{id}/escalate")
    @PreAuthorize("hasAuthority('RESOLUTION_ESCALATE') or hasAuthority('ALL_RESOLUTION_UPDATE')")
    public ResponseEntity<ResolutionDto> escalate(@PathVariable Long id) {
        return ResponseEntity.ok(resolutionService.escalate(id));
    }

    /** Владелец подаёт жалобу на гостя (фиксация без запроса денег). */
    @PutMapping("/{id}/complaint")
    @PreAuthorize("hasAuthority('RESOLUTION_COMPLAINT') or hasAuthority('ALL_RESOLUTION_UPDATE')")
    public ResponseEntity<ResolutionDto> recordComplaint(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintRequest request) {
        return ResponseEntity.ok(resolutionService.recordComplaint(id, request));
    }

    /** Закрытие окна разрешения: админ — всегда; владелец — только из статуса REFUSED (закрыть без эскалации). */
    @PutMapping("/{id}/close")
    @PreAuthorize("hasAuthority('RESOLUTION_READ') or hasAuthority('ALL_RESOLUTION_UPDATE')")
    public ResponseEntity<ResolutionDto> close(@PathVariable Long id) {
        return ResponseEntity.ok(resolutionService.close(id));
    }

    /** Решение по эскалации (только админ): существенное происшествие — да (назначить обязательную выплату гостю) или нет (урегулировать без взыскания, уведомить владельца). */
    @PutMapping("/{id}/resolve-escalation")
    @PreAuthorize("hasAuthority('ALL_RESOLUTION_UPDATE')")
    public ResponseEntity<ResolutionDto> resolveEscalation(
            @PathVariable Long id,
            @Valid @RequestBody ResolveEscalationRequest request) {
        return ResponseEntity.ok(resolutionService.resolveEscalation(id, request));
    }
}
