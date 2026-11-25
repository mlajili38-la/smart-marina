package tn.iset.marina.consumption_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import tn.iset.marina.consumption_service.entity.ConsumptionType;

import java.math.BigDecimal;
import java.time.Instant;

public record ConsumptionRequest(
        @NotNull @Positive Long pedestalId,
        @NotNull ConsumptionType type,
        @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal quantity,
        @Size(max = 100) String boatName,
        @NotNull @PastOrPresent Instant startedAt,
        @NotNull @PastOrPresent Instant endedAt
) {

    /** La fin de la période doit être postérieure à son début. */
    @JsonIgnore
    @AssertTrue(message = "endedAt doit être postérieur à startedAt")
    public boolean isPeriodValid() {
        return startedAt == null || endedAt == null || endedAt.isAfter(startedAt);
    }
}
