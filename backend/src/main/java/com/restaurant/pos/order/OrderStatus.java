package com.restaurant.pos.order;

public enum OrderStatus {
    DRAFT,
    SUBMITTED,
    FIRED,
    PREPARING,
    READY,
    COMPLETED,
    CANCELLED;

    /**
     * What PATCH /status may do: repeat the current status, submit a draft, or move along the kitchen path.
     * FIRED, COMPLETED and CANCELLED are reached through fire and payment, not through PATCH.
     */
    public boolean canPatchTo(OrderStatus next) {
        return next == this
                || this == DRAFT && next == SUBMITTED
                || this == FIRED && next == PREPARING
                || this == PREPARING && next == READY;
    }
}
