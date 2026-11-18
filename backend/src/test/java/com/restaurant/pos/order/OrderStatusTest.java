package com.restaurant.pos.order;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static com.restaurant.pos.order.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    private static final Map<OrderStatus, OrderStatus> PATCHABLE =
            Map.of(DRAFT, SUBMITTED, FIRED, PREPARING, PREPARING, READY);

    static Stream<Arguments> everyPair() {
        return Stream.of(values()).flatMap(from -> Stream.of(values())
                .map(to -> Arguments.of(from, to, from == to || PATCHABLE.get(from) == to)));
    }

    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @MethodSource("everyPair")
    void canPatchTo_allowsOnlyTheKitchenPathAndNoOps(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canPatchTo(to)).isEqualTo(allowed);
    }
}
