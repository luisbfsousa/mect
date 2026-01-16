package com.shophub.service;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> getSalesAnalytics(LocalDate startDate, LocalDate endDate, Integer categoryId) {
        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        Map<String, Object> result = new HashMap<>();

        // Total revenue and orders in range (optional category filter)
        String revenueSql = "SELECT COALESCE(SUM(o.total_amount),0) AS revenue, COUNT(o.order_id) AS orders " +
                "FROM orders o " +
                (categoryId != null ? "JOIN order_items oi ON oi.order_id = o.order_id JOIN products p ON p.product_id = oi.product_id " : "") +
                "WHERE o.created_at BETWEEN :start AND :end " +
                (categoryId != null ? "AND p.category_id = :categoryId " : "");

        Query revenueQuery = entityManager.createNativeQuery(revenueSql);
        revenueQuery.setParameter("start", start);
        revenueQuery.setParameter("end", end);
        if (categoryId != null) revenueQuery.setParameter("categoryId", categoryId);
        Object[] revenueRow = (Object[]) revenueQuery.getSingleResult();
        result.put("revenue", revenueRow[0]);
        result.put("totalOrders", revenueRow[1]);

        // All sales products by quantity (no limit)
        String allSalesSql = "SELECT p.product_id, p.name, SUM(oi.quantity) AS qty, SUM(oi.subtotal) AS sales " +
                "FROM order_items oi " +
                "JOIN orders o ON o.order_id = oi.order_id " +
                "JOIN products p ON p.product_id = oi.product_id " +
                "WHERE o.created_at BETWEEN :start AND :end " +
                (categoryId != null ? "AND p.category_id = :categoryId " : "") +
                "GROUP BY p.product_id, p.name " +
                "ORDER BY qty DESC";

        Query allSalesQuery = entityManager.createNativeQuery(allSalesSql);
        allSalesQuery.setParameter("start", start);
        allSalesQuery.setParameter("end", end);
        if (categoryId != null) allSalesQuery.setParameter("categoryId", categoryId);
        @SuppressWarnings("unchecked")
        List<Object[]> allSalesRows = allSalesQuery.getResultList();
        result.put("allSalesProducts", allSalesRows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("productId", r[0]);
            m.put("name", r[1]);
            m.put("quantity", r[2]);
            m.put("sales", r[3]);
            return m;
        }).toList());

        // Best-selling products (top 3)
        String bestSql = "SELECT p.product_id, p.name, SUM(oi.quantity) AS qty, SUM(oi.subtotal) AS sales " +
                "FROM order_items oi " +
                "JOIN orders o ON o.order_id = oi.order_id " +
                "JOIN products p ON p.product_id = oi.product_id " +
                "WHERE o.created_at BETWEEN :start AND :end " +
                (categoryId != null ? "AND p.category_id = :categoryId " : "") +
                "GROUP BY p.product_id, p.name " +
                "ORDER BY qty DESC LIMIT 3";

        Query bestQuery = entityManager.createNativeQuery(bestSql);
        bestQuery.setParameter("start", start);
        bestQuery.setParameter("end", end);
        if (categoryId != null) bestQuery.setParameter("categoryId", categoryId);
        @SuppressWarnings("unchecked")
        List<Object[]> bestRows = bestQuery.getResultList();
        result.put("bestSellingProducts", bestRows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("productId", r[0]);
            m.put("name", r[1]);
            m.put("quantity", r[2]);
            m.put("sales", r[3]);
            return m;
        }).toList());

        // Customer demographics: by city (from order shipping_address JSONB)
        String demoSql = "SELECT o.shipping_address->>'city' AS city, COUNT(o.order_id) AS orders " +
                "FROM orders o " +
                (categoryId != null ? "JOIN order_items oi ON oi.order_id = o.order_id JOIN products p ON p.product_id = oi.product_id " : "") +
                "WHERE o.created_at BETWEEN :start AND :end " +
                "AND o.shipping_address IS NOT NULL " +
                (categoryId != null ? "AND p.category_id = :categoryId " : "") +
                "GROUP BY o.shipping_address->>'city' ORDER BY orders DESC";

        Query demoQuery = entityManager.createNativeQuery(demoSql);
        demoQuery.setParameter("start", start);
        demoQuery.setParameter("end", end);
        if (categoryId != null) demoQuery.setParameter("categoryId", categoryId);
        @SuppressWarnings("unchecked")
        List<Object[]> demoRows = demoQuery.getResultList();
        result.put("customerDemographics", demoRows.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("city", r[0]);
            m.put("orders", r[1]);
            return m;
        }).toList());

        return result;
    }
}
