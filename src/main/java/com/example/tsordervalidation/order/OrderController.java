package com.example.tsordervalidation.order;

import com.example.tsordervalidation.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class OrderController {
    @Autowired
    private OrderRepository orderRepository;

    //all orders
    @GetMapping("v1/orders")
    public List<Order> getAllOrders(){
        return this.orderRepository.findAll();
    }

    //all clients order
    @GetMapping("v1/orders/{clientId}")
    public List<Order> getAllOrdersByClientId (@PathVariable(value="clientId") Long clientId) throws ResourceNotFoundException {
        List<Order> orderList = orderRepository.findAllByClientId(clientId);
        return orderList;
    }

}
