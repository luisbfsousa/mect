package com.shophub.controller;

import com.shophub.dto.PackingSlipDTO;
import com.shophub.dto.WarehouseDeliverOrderRequest;
import com.shophub.dto.WarehouseShipOrderRequest;
import com.shophub.model.Order;
import com.shophub.service.OrderService;
import com.shophub.service.PackingSlipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('WAREHOUSE_STAFF', 'warehouse-staff')")
public class WarehouseController {

    private final OrderService orderService;
    private final PackingSlipService packingSlipService;

    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customer
    ) {
        List<Order> orders = orderService.getAllOrders(status, customer);
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/orders/{orderId}/ship")
    public ResponseEntity<Order> markOrderAsShipped(
            @PathVariable Integer orderId,
            @Valid @RequestBody WarehouseShipOrderRequest request
    ) {
        Order order = orderService.markAsShipped(
                orderId,
                request.getTrackingNumber(),
                request.getShippingProvider()
        );
        return ResponseEntity.ok(order);
    }

    @PatchMapping("/orders/{orderId}/deliver")
    public ResponseEntity<Order> markOrderAsDelivered(
            @PathVariable Integer orderId,
            @Valid @RequestBody WarehouseDeliverOrderRequest request
    ) {
        Order order = orderService.markAsDelivered(orderId, Boolean.TRUE.equals(request.getConfirm()));
        return ResponseEntity.ok(order);
    }

    // testing endpoint to inspect orders irrespective of roles
    @GetMapping("/orders/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Order>> getAllOrdersDebug(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customer
    ) {
        List<Order> orders = orderService.getAllOrders(status, customer);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/{orderId}/packing-slip")
    public ResponseEntity<PackingSlipDTO> getPackingSlip(@PathVariable Integer orderId) {
        PackingSlipDTO packingSlip = packingSlipService.generatePackingSlipData(orderId);
        return ResponseEntity.ok(packingSlip);
    }

    @GetMapping("/orders/{orderId}/packing-slip/pdf")
    public ResponseEntity<byte[]> downloadPackingSlipPDF(@PathVariable Integer orderId) {
        byte[] pdfBytes = packingSlipService.generatePackingSlipPDF(orderId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=packing-slip-" + orderId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
