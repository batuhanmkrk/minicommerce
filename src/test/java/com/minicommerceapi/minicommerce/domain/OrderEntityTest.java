package com.minicommerceapi.minicommerce.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderEntityTest {

    @Test
    void addItem_shouldAddToListAndSetBackReference() {
        Order order = new Order();
        OrderItem item = new OrderItem();

        assertEquals(0, order.getItems().size());
        assertNull(item.getOrder());

        order.addItem(item);

        assertEquals(1, order.getItems().size());
        assertSame(item, order.getItems().get(0));
        assertSame(order, item.getOrder()); // back-reference set edilmeli
    }

    @Test
    void addItem_shouldAllowMultipleItems() {
        Order order = new Order();
        OrderItem i1 = new OrderItem();
        OrderItem i2 = new OrderItem();

        order.addItem(i1);
        order.addItem(i2);

        assertEquals(2, order.getItems().size());
        assertSame(order, i1.getOrder());
        assertSame(order, i2.getOrder());
    }
}
