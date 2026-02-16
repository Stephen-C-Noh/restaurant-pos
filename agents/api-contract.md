# API Contract — Shared Between Backend & Frontend

> **IMPORTANT:** This is the single source of truth for API shapes.
> - **Backend Dev** must update this file when adding/changing endpoints or DTOs.
> - **Frontend Dev** must read this file before integrating any endpoint.
> - **QA agents** must validate against these contracts.

## REST Endpoints

### Menu

| Method | Path | Request Body | Response | Status Codes |
|--------|------|-------------|----------|-------------|
| GET | `/api/menu/items` | — | `MenuItem[]` | 200 |
| GET | `/api/menu/items/active` | — | `MenuItem[]` | 200 |
| GET | `/api/menu/items/{id}` | — | `MenuItem` | 200, 404 |
| POST | `/api/menu/items` | `MenuItem` (body) | `MenuItem` | 201, 400 |
| PUT | `/api/menu/items/{id}` | `MenuItem` (body) | `MenuItem` | 200, 404, 400 |
| DELETE | `/api/menu/items/{id}` | — | — | 204, 404 |
| GET | `/api/menu/categories` | — | `MenuCategory[]` | 200 |
| POST | `/api/menu/categories` | `MenuCategory` (body) | `MenuCategory` | 201, 400 |

### Orders

| Method | Path | Request Body | Response | Status Codes |
|--------|------|-------------|----------|-------------|
| GET | `/api/orders` | — | `OrderResponse[]` | 200 |
| GET | `/api/orders/{id}` | — | `OrderResponse` | 200, 404 |
| GET | `/api/orders/active` | — | `OrderResponse[]` | 200 |
| POST | `/api/orders` | `CreateOrderRequest` | `OrderResponse` | 201, 400 |
| PATCH | `/api/orders/{id}/status` | `{ "status": OrderStatus }` | `OrderResponse` | 200, 400, 404 |
| POST | `/api/orders/{id}/fire` | — | `OrderResponse` | 200, 400, 404 |
| PATCH | `/api/orders/{orderId}/items/{itemId}/status` | `UpdateItemStatusRequest` | `OrderResponse` | 200, 400, 404 |

## DTO Shapes

### CreateOrderRequest
```json
{
  "orderType": "DINE_IN | TAKEOUT | DELIVERY",
  "tableId": "uuid (nullable)",
  "serverId": "uuid (nullable)",
  "guestCount": "integer (nullable)",
  "notes": "string (nullable)",
  "items": [
    {
      "menuItemId": "uuid (required)",
      "quantity": "integer (required)",
      "specialInstructions": "string (nullable)"
    }
  ]
}
```

### OrderResponse
```json
{
  "id": "uuid",
  "orderNumber": "string (e.g. ORD-001)",
  "orderType": "DINE_IN | TAKEOUT | DELIVERY",
  "status": "DRAFT | SUBMITTED | FIRED | PREPARING | READY | COMPLETED | CANCELLED",
  "tableId": "uuid (nullable)",
  "serverId": "uuid (nullable)",
  "guestCount": "integer (nullable)",
  "subtotal": "decimal",
  "taxAmount": "decimal",
  "total": "decimal",
  "notes": "string (nullable)",
  "items": [ "OrderItemResponse" ],
  "createdAt": "ISO-8601 timestamp",
  "updatedAt": "ISO-8601 timestamp",
  "firedAt": "ISO-8601 timestamp (nullable)"
}
```

### OrderItemResponse
```json
{
  "id": "uuid",
  "menuItemId": "uuid",
  "menuItemName": "string",
  "quantity": "integer",
  "unitPrice": "decimal",
  "subtotal": "decimal",
  "specialInstructions": "string (nullable)",
  "status": "PENDING | FIRED | PREPARING | READY | SERVED",
  "section": "COLD | GRILL | FRYER | SAUTE | APPETIZER | DESSERT"
}
```

### UpdateItemStatusRequest
```json
{
  "status": "PENDING | FIRED | PREPARING | READY | SERVED"
}
```

### MenuItem
```json
{
  "id": "uuid",
  "categoryId": "uuid",
  "name": "string",
  "description": "string (nullable)",
  "kitchenSection": "COLD | GRILL | FRYER | SAUTE | APPETIZER | DESSERT",
  "sku": "string",
  "basePrice": "decimal",
  "taxRate": "decimal (default 0.05)",
  "prepTimeMinutes": "integer",
  "active": "boolean",
  "imageUrl": "string (nullable)"
}
```

### MenuCategory
```json
{
  "id": "uuid",
  "name": "string",
  "displayOrder": "integer",
  "active": "boolean"
}
```

## WebSocket

| Protocol | Endpoint | Subscribe Topic | Message Shape |
|----------|----------|----------------|---------------|
| STOMP over SockJS | `/ws` | `/topic/orders` | `OrderResponse` |

## Kafka Topics

| Topic | Producer | Consumer | Event Shape |
|-------|----------|----------|-------------|
| `order-fired-events` | OrderService (via EventPublisher) | KitchenDisplayService | `OrderFiredEvent` |
| `item-status-events` | OrderService (via EventPublisher) | KitchenDisplayService | `ItemStatusChangedEvent` |

## Enums

### OrderStatus
`DRAFT` → `SUBMITTED` → `FIRED` → `PREPARING` → `READY` → `COMPLETED`
(also: `CANCELLED` from any state)

### OrderItemStatus
`PENDING` → `FIRED` → `PREPARING` → `READY` → `SERVED`

### OrderType
`DINE_IN`, `TAKEOUT`, `DELIVERY`

### KitchenSection
`COLD`, `GRILL`, `FRYER`, `SAUTE`, `APPETIZER`, `DESSERT`

---

## Change Log

| Date | Change | Agent |
|------|--------|-------|
| 2026-02-16 | Initial contract created from existing codebase | Setup |
