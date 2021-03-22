package com.example.tsordervalidation.service;

import com.example.tsordervalidation.marketdata.MarketData;
import com.example.tsordervalidation.order.Order;
import com.example.tsordervalidation.order.OrderRepository;
import com.example.tsordervalidation.ordervalidation.Acknowledgement;
import com.example.tsordervalidation.ordervalidation.OrderRequest;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;

@Service
public class OrderValidationService {
    private String market_data_url = "https://exchange2.matraining.com/md/";
    @Autowired
    private OrderRepository orderRepository;

    private final Logger log = LoggerFactory.getLogger(OrderValidationService.class);

    public Acknowledgement checkOrderValidity(OrderRequest request) {

        boolean isValid = false;
        String message = "";


        // get market data
        RestTemplate restTemplate = new RestTemplate();
        MarketData marketData = null;
        try {
            marketData = restTemplate.getForObject(market_data_url +
                    request.getProduct().toLowerCase().replace("\"", ""), MarketData.class);

        } catch (Exception e) {
            // product is not on the market
            message = "Product does not exist in the market";
            return setAckValidity(request, false, message);
        }

        log.info(marketData.toString());

        // Selling
        if (request.getSide() == "SELL") {
            // check if client owns product // to be obtained from db
            if (request.getProduct() == "IBM") {
                message = "IBM does not exist in your portfolio";
            } else if (request.getPrice() < marketData.getASK_PRICE() - marketData.getMAX_PRICE_SHIFT()) {
                message = "High chance of price not being accepted";
            }

        } else {
            if (request.getPrice() * request.getQuantity() > 6000) {
                message = "Insufficient funds";
                //check if product exist and
                // check if buy price is reasonable

            } else if (request.getPrice() > marketData.getBID_PRICE() + marketData.getMAX_PRICE_SHIFT()) {
                message = "High chance of price not being accepted";
            }
        }

        if (message == "") {
            message = "Order submitted";
            isValid = true;
        }

        return setAckValidity(request, isValid, message);
    }

    private Acknowledgement setAckValidity(OrderRequest orderRequest, boolean isValid, String message) {

        // save order to db.
        Order newOrder = orderRepository
                .save(new Order(
                        orderRequest.getProduct(),
                        orderRequest.getSide(),
                        orderRequest.getPrice(),
                        orderRequest.getQuantity(),
                        orderRequest.getClientId(),
                        isValid
                ));

        Acknowledgement acknowledgement = new Acknowledgement();
        acknowledgement.setComment(message);
        acknowledgement.setIsValid(isValid);
        acknowledgement.setOrderId(newOrder.getId());

        // TODO://report trade activity
        //reporting service OrderAcitvity(orderId, action, status, dataTime)

        //ToDo://proceed to trade engine if valid.
        return acknowledgement;
    }
}
