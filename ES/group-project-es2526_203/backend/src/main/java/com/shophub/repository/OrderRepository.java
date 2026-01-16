package com.shophub.repository;

import com.shophub.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
    
    Optional<Order> findByOrderIdAndUserId(Integer orderId, String userId);
    
    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.orderStatus = :status) AND " +
           "(:search IS NULL OR CAST(o.orderId AS string) LIKE %:search% OR " +
           "o.userId LIKE %:search% OR " +
           "o.trackingNumber LIKE %:search%) " +
           "ORDER BY o.createdAt DESC")
    List<Order> findAllWithFilters(@Param("status") String status, 
                                   @Param("search") String search);
    
    @Query("SELECT COUNT(o) as totalOrders, " +
           "SUM(CASE WHEN o.orderStatus = 'pending' THEN 1 ELSE 0 END) as pendingOrders, " +
           "SUM(CASE WHEN o.orderStatus = 'processing' THEN 1 ELSE 0 END) as processingOrders, " +
           "SUM(CASE WHEN o.orderStatus = 'shipped' THEN 1 ELSE 0 END) as shippedOrders, " +
           "SUM(CASE WHEN o.orderStatus = 'delivered' THEN 1 ELSE 0 END) as deliveredOrders, " +
           "COALESCE(SUM(o.totalAmount), 0) as totalRevenue " +
           "FROM Order o")
    Object getOrderStatistics();
}