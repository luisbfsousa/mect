package com.shophub.controller;

import com.shophub.dto.CreateOrderRequest;
import com.shophub.dto.UpdateOrderStatusRequest;
import com.shophub.model.Order;
import com.shophub.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
//@CrossOrigin(origins = "*")
public class OrderController {
    
    private final OrderService orderService;
    
    @GetMapping
    public ResponseEntity<List<Order>> getUserOrders(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(orderService.getOrderById(userId, id));
    }
    
    @PostMapping
    public ResponseEntity<Order> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request) {
        String userId = jwt.getSubject();
        Order order = orderService.createOrder(userId, request, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.getStatus()));
    }
}