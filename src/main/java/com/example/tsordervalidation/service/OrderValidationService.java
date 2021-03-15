package com.example.tsordervalidation.service;

import com.example.tsordervalidation.ordervalidation.Acknowledgement;
import com.example.tsordervalidation.ordervalidation.OrderRequest;
import org.springframework.stereotype.Service;

@Service
public class OrderValidationService {

    public Acknowledgement checkOrderValidity(OrderRequest request) {System.err.println("+++++++++++service+++++++++++++++");
        Acknowledgement acknowledgement = new Acknowledgement();
        acknowledgement.setOrderId("10");
        acknowledgement.setClientId("1");

        if(request.getAmount() > 1000) {
            acknowledgement.setIsValid(false);
        } else {
            acknowledgement.setIsValid(true);
        }
        return acknowledgement;
    }
}
