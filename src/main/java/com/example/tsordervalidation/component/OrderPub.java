package com.example.tsordervalidation.component;

import com.example.tsordervalidation.order.Order;

public interface OrderPub {
    public void publish(Order message);
}
