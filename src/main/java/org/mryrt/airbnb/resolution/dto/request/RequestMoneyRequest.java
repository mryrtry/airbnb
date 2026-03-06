package org.mryrt.airbnb.resolution.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestMoneyRequest {

    @NotNull(message = "Resolution.amountRequested не может быть пустым")
    @DecimalMin(value = "0.01", message = "Resolution.amountRequested должен быть положительным")
    private BigDecimal amountRequested;
}
