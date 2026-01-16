package com.shophub.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.shophub.dto.PackingSlipDTO;
import com.shophub.dto.PackingSlipItemDTO;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.Order;
import com.shophub.model.OrderItem;
import com.shophub.repository.OrderItemRepository;
import com.shophub.repository.OrderRepository;
import com.shophub.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackingSlipService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    
    @Transactional(readOnly = true)
    public PackingSlipDTO generatePackingSlipData(Integer orderId) {
        log.info("Generating packing slip data for order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        
        List<PackingSlipItemDTO> slipItems = new ArrayList<>();
        Integer totalQuantity = 0;
        
        for (OrderItem item : items) {
            String productName = productRepository.findById(item.getProductId())
                    .map(p -> p.getName())
                    .orElse("Unknown Product");
            
            PackingSlipItemDTO slipItem = PackingSlipItemDTO.builder()
                    .productId(item.getProductId())
                    .productName(productName)
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .subtotal(item.getSubtotal())
                    .build();
            
            slipItems.add(slipItem);
            totalQuantity += item.getQuantity();
        }
        
        // Fetch user information for customer name
        String firstName = "";
        String lastName = "";
        try {
            com.shophub.model.User user = userService.getUserById(order.getUserId());
            if (user != null) {
                firstName = user.getFirstName() != null ? user.getFirstName() : "";
                lastName = user.getLastName() != null ? user.getLastName() : "";
            }
        } catch (Exception e) {
            log.warn("Failed to fetch user information for order: {}", orderId, e);
        }
        
        PackingSlipDTO packingSlip = PackingSlipDTO.builder()
                .orderId(order.getOrderId())
                .orderStatus(order.getOrderStatus())
                .userId(order.getUserId())
                .firstName(firstName)
                .lastName(lastName)
                .shippingAddress(order.getShippingAddress())
                .trackingNumber(order.getTrackingNumber())
                .shippingProvider(order.getShippingProvider())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .estimatedDeliveryDate(order.getEstimatedDeliveryDate())
                .items(slipItems)
                .itemCount(slipItems.size())
                .totalQuantity(totalQuantity)
                .build();
        
        log.info("Packing slip data generated successfully for order: {}", orderId);
        return packingSlip;
    }
    
    @Transactional(readOnly = true)
    public byte[] generatePackingSlipPDF(Integer orderId) {
        log.info("Generating PDF packing slip for order: {}", orderId);
        
        PackingSlipDTO packingSlip = generatePackingSlipData(orderId);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);
            pdfDocument.setDefaultPageSize(PageSize.A4);
            
            Document document = new Document(pdfDocument);
            document.setMargins(40, 40, 40, 40);
            
            // Header - Company/Shop Title
            Paragraph header = new Paragraph("PACKING SLIP")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(header);
            
            // Order Info Section
            Table orderInfoTable = new Table(new float[]{1f, 1f});
            orderInfoTable.setWidth(UnitValue.createPercentValue(100));
            
            // Left column
            Cell orderIdCell = new Cell()
                    .add(new Paragraph("Order ID").setBold())
                    .add(new Paragraph(packingSlip.getOrderId().toString()).setFontSize(14));
            orderInfoTable.addCell(orderIdCell);
            
            // Right column
            Cell dateCell = new Cell()
                    .add(new Paragraph("Order Date").setBold())
                    .add(new Paragraph(packingSlip.getCreatedAt().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))));
            orderInfoTable.addCell(dateCell);
            
            document.add(orderInfoTable);
            document.add(new Paragraph(" "));
            
            // Tracking Information
            Table trackingTable = new Table(new float[]{1f, 1f});
            trackingTable.setWidth(UnitValue.createPercentValue(100));
            
            Cell trackingLabelCell = new Cell()
                    .add(new Paragraph("Tracking Number").setBold())
                    .add(new Paragraph(packingSlip.getTrackingNumber() != null ? 
                        packingSlip.getTrackingNumber() : "N/A"));
            trackingTable.addCell(trackingLabelCell);
            
            Cell providerLabelCell = new Cell()
                    .add(new Paragraph("Shipping Provider").setBold())
                    .add(new Paragraph(packingSlip.getShippingProvider() != null ? 
                        packingSlip.getShippingProvider() : "N/A"));
            trackingTable.addCell(providerLabelCell);
            
            document.add(trackingTable);
            document.add(new Paragraph(" "));
            
            // Shipping Address Section
            Paragraph addressTitle = new Paragraph("Ship To:")
                    .setBold()
                    .setFontSize(12);
            document.add(addressTitle);
            
            if (packingSlip.getShippingAddress() != null) {
                String addressStr = formatShippingAddress(packingSlip.getShippingAddress());
                document.add(new Paragraph(addressStr));
            } else {
                document.add(new Paragraph("Address information not available"));
            }
            
            document.add(new Paragraph(" "));
            
            // Items Table Header
            Paragraph itemsTitle = new Paragraph("Items to Pack:")
                    .setBold()
                    .setFontSize(12);
            document.add(itemsTitle);
            
            // Items Table
            Table itemsTable = new Table(new float[]{1f, 1f, 1f, 1.2f, 1f});
            itemsTable.setWidth(UnitValue.createPercentValue(100));
            
            // Header row
            Color headerColor = new DeviceGray(0.7f);
            
            Cell headerProduct = new Cell()
                    .add(new Paragraph("Product Name").setBold())
                    .setBackgroundColor(headerColor)
                    .setTextAlignment(TextAlignment.CENTER);
            itemsTable.addHeaderCell(headerProduct);
            
            Cell headerProductId = new Cell()
                    .add(new Paragraph("Product ID").setBold())
                    .setBackgroundColor(headerColor)
                    .setTextAlignment(TextAlignment.CENTER);
            itemsTable.addHeaderCell(headerProductId);
            
            Cell headerQty = new Cell()
                    .add(new Paragraph("Qty").setBold())
                    .setBackgroundColor(headerColor)
                    .setTextAlignment(TextAlignment.CENTER);
            itemsTable.addHeaderCell(headerQty);
            
            Cell headerPrice = new Cell()
                    .add(new Paragraph("Unit Price").setBold())
                    .setBackgroundColor(headerColor)
                    .setTextAlignment(TextAlignment.CENTER);
            itemsTable.addHeaderCell(headerPrice);
            
            Cell headerSubtotal = new Cell()
                    .add(new Paragraph("Subtotal").setBold())
                    .setBackgroundColor(headerColor)
                    .setTextAlignment(TextAlignment.CENTER);
            itemsTable.addHeaderCell(headerSubtotal);
            
            // Data rows
            for (PackingSlipItemDTO item : packingSlip.getItems()) {
                itemsTable.addCell(new Cell().add(new Paragraph(item.getProductName())));
                itemsTable.addCell(new Cell().add(new Paragraph(item.getProductId().toString())).setTextAlignment(TextAlignment.CENTER));
                itemsTable.addCell(new Cell().add(new Paragraph(item.getQuantity().toString())).setTextAlignment(TextAlignment.CENTER));
                itemsTable.addCell(new Cell().add(new Paragraph(formatCurrency(item.getUnitPrice()))).setTextAlignment(TextAlignment.RIGHT));
                itemsTable.addCell(new Cell().add(new Paragraph(formatCurrency(item.getSubtotal()))).setTextAlignment(TextAlignment.RIGHT));
            }
            
            document.add(itemsTable);
            document.add(new Paragraph(" "));
            
            // Summary Section
            Table summaryTable = new Table(new float[]{2f, 1f});
            summaryTable.setWidth(UnitValue.createPercentValue(60));
            summaryTable.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            
            summaryTable.addCell(new Cell().add(new Paragraph("Total Items:").setBold()));
            summaryTable.addCell(new Cell().add(new Paragraph(packingSlip.getItemCount().toString()).setTextAlignment(TextAlignment.RIGHT)));
            
            summaryTable.addCell(new Cell().add(new Paragraph("Total Quantity:").setBold()));
            summaryTable.addCell(new Cell().add(new Paragraph(packingSlip.getTotalQuantity().toString()).setTextAlignment(TextAlignment.RIGHT)));
            
            summaryTable.addCell(new Cell().add(new Paragraph("Total Amount:").setBold().setFontSize(12)));
            summaryTable.addCell(new Cell().add(new Paragraph(formatCurrency(packingSlip.getTotalAmount())).setBold().setFontSize(12)).setTextAlignment(TextAlignment.RIGHT));
            
            document.add(summaryTable);
            document.add(new Paragraph(" "));
            
            // Footer
            Paragraph footer = new Paragraph("Please verify all items before sealing the package.")
                    .setFontSize(9)
                    .setItalic()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(footer);
            
            document.close();
            
            log.info("PDF packing slip generated successfully for order: {}", orderId);
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("Error generating packing slip PDF for order: {}", orderId, e);
            throw new RuntimeException("Failed to generate packing slip PDF", e);
        }
    }
    
    private String formatShippingAddress(Object addressObj) {
        if (addressObj == null) {
            return "Address information not available";
        }
        
        if (addressObj instanceof java.util.Map) {
            java.util.Map<String, Object> address = (java.util.Map<String, Object>) addressObj;
            StringBuilder sb = new StringBuilder();
            
            if (address.containsKey("street")) {
                sb.append(address.get("street"));
            }
            if (address.containsKey("city")) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(address.get("city"));
            }
            if (address.containsKey("state")) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(address.get("state"));
            }
            if (address.containsKey("zipCode")) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(address.get("zipCode"));
            }
            if (address.containsKey("country")) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(address.get("country"));
            }
            
            return sb.length() > 0 ? sb.toString() : "Address information not available";
        }
        
        return addressObj.toString();
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        return String.format("$%.2f", amount);
    }
}
