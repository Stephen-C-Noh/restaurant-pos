package com.restaurant.pos.table;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTableRequest(
        @NotBlank @Size(max = 10) String tableNumber,
        @Min(1) int capacity,
        @Size(max = 50) String zone
) {}
