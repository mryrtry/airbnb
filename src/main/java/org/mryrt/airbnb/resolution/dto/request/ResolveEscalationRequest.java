package org.mryrt.airbnb.resolution.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResolveEscalationRequest {

    /** true — существенное происшествие, назначить гостю обязательную выплату; false — урегулировать без взыскания, уведомить владельца. */
    private Boolean substantialIncident;
}
