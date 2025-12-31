package com.restaurant.pos.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
/* @Data is Lombok's most powerful annotation - it generates ALL of these automatically:

✅ Getters for all fields → getStatus(), getId(), etc.
✅ Setters for all non-final fields → setStatus(), setId(), etc.
✅ toString() method → For debugging/logging
✅ equals() method → For comparing objects
✅ hashCode() method → For using in HashMaps/Sets
✅ Required args constructor → For final fields */
@NoArgsConstructor
@AllArgsConstructor
public class UpdateItemStatusRequest {
    private OrderItemStatus status;
}