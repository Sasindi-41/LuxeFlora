package lk.sasindi.luxeflora.model;


import com.google.firebase.Timestamp;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    private String orderId;
    private String userId;
    private double totalAmount;
    private String status;
    private Timestamp orderDate;
    private List<OrderItem> orderItems;
    private Address shippingAddress;
    private Address billingAddress;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItem{
        private String productId;
        private String productName;
        private String productImage;
        private double unitPrice;
        private int quantity;
        private List<OrderItem.Attribute> attributes;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Attribute{
            private String name;
            private String value;
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Address{
        private String name;
        private String email;
        private String contact;
        private String address1;
        private String address2;
        private String city;
        private String postcode;
    }

    public int getTotalQuantity() {
        int total = 0;

        if (orderItems != null) {
            for (Order.OrderItem item : orderItems) {
                total += item.getQuantity();
            }
        }

        return total;
    }
}


