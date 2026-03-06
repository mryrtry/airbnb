package org.mryrt.airbnb.auth.repository.filter;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class UserFilter {

    private String username;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAtAfter;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAtBefore;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate updatedAtAfter;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate updatedAtBefore;

}