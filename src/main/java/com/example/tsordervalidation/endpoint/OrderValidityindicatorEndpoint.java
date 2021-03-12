package com.example.tsordervalidation.endpoint;

import com.example.tsordervalidation.ordervalidation.Acknowledgement;
import com.example.tsordervalidation.ordervalidation.OrderRequest;
import com.example.tsordervalidation.service.OrderValidationService;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class OrderValidityindicatorEndpoint {

    private static final String NAMESPACE ="http://www.example.com/tsordervalidation/ordervalidation";

    private OrderValidationService service;

    @PayloadRoot(namespace = NAMESPACE, localPart = "OrderRequest")
    @ResponsePayload
    public Acknowledgement getOrdervalidityStatus(@RequestPayload OrderRequest request) {
        return service.checkOrderValidity(request);
    }
}
