package com.example.tsordervalidation.service;

import com.example.tsordervalidation.component.OrderPublisher;
import com.example.tsordervalidation.marketdata.ExchangeDao;
import com.example.tsordervalidation.marketdata.MarketData;
import com.example.tsordervalidation.order.Order;
import com.example.tsordervalidation.order.OrderRepository;
import com.example.tsordervalidation.ordervalidation.Acknowledgement;
import com.example.tsordervalidation.ordervalidation.OrderRequest;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderValidationService {
    @Autowired
    private ExchangeDao exchangeData;
    @Autowired
    private OrderPublisher orderPublisher;
    private String market_data_url = "https://exchange2.matraining.com/md/";
    @Autowired
    private OrderRepository orderRepository;

    private final Logger log = LoggerFactory.getLogger(OrderValidationService.class);

    private List<MarketData> getMarketDataList(String ticker) {

        String engineMdUrl = String.format("http://localhost:8083/v0/api/md-by-ticker/%s",ticker);
        //
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<List<MarketData>> responseEntity =
                restTemplate.exchange(
                        engineMdUrl,
                        HttpMethod.GET, null,
                        new ParameterizedTypeReference<List<MarketData>>() {
                        }
                );
        List<MarketData> MarketDataList = responseEntity.getBody();

        return MarketDataList.stream().collect(Collectors.toList());
    }

    public Acknowledgement checkOrderValidity(OrderRequest request) {
        //starter list, make get request when the app start, update the list when the sub data comes
        //container[ex1,ex2,exy]

        boolean isValid = false;
        String message = "";
        boolean isHighPrice = true;
        boolean isLowPrice = true;
        boolean isOverMarketQuantity = true;
        boolean isOverBuyLimit = false;
        boolean isOverSellLimit = false;

        //Todo: to be continued working with all exchanges

        // get market data
        List<MarketData> MarketDataList = getMarketDataList(request.getProduct());
        MarketData marketData = null;
        for (MarketData md : MarketDataList) {
            if(isHighPrice && !(request.getPrice() > md.getLAST_TRADED_PRICE() + md.getMAX_PRICE_SHIFT())) {
                isHighPrice = !isHighPrice;
                marketData = md;
            }
            if(isLowPrice && !(request.getPrice() < md.getLAST_TRADED_PRICE() - md.getMAX_PRICE_SHIFT())) {
                isLowPrice = !isLowPrice;
                marketData = md;
            }
            if(isOverMarketQuantity && !(request.getQuantity() > md.getSELL_LIMIT())) {
               isOverMarketQuantity = !isOverMarketQuantity;
                marketData = md;
            }
            if((request.getQuantity() > md.getBUY_LIMIT())) {
                isOverBuyLimit = true;
                marketData = md;
            } else {
                isOverBuyLimit = false;
            }
            if((request.getQuantity() > md.getSELL_LIMIT())) {
                isOverSellLimit = true;
                marketData = md;
            } else {
                isOverSellLimit = false;
            }
        }


        if (MarketDataList.isEmpty()) {
            return setAckValidity(request, false, "The product does not exist in the market");
        }
        if (request.getQuantity() < 1) {
            return setAckValidity(request, false, "Quantity can not be less than 1");
        }
        log.info(MarketDataList.toString());


        // check if buy price is reasonable
        if (isHighPrice) {
            message = request.getPrice() + " is too high "+(marketData.getLAST_TRADED_PRICE() + marketData.getMAX_PRICE_SHIFT())+" for the current market values for this product";

        } else if (isLowPrice) {
            message = request.getPrice() + " is too low  "+(marketData.getLAST_TRADED_PRICE() + marketData.getMAX_PRICE_SHIFT())+" for the current market values for this product";

        } else if (request.getSide().equals("SELL") && (request.getProduct().equals("IBM1"))) { // actual client stocks
            // Selling
            // check if client owns product // to be obtained from db
            message = "IBM does not exist in your portfolio";

        } else if (request.getSide().equals("SELL") && (isOverSellLimit)) { // actual client stocks
            // Selling
            // check if client owns product // to be obtained from db
            message =  "You can not sell more than "+marketData.getSELL_LIMIT();;

        } else if (request.getSide().equals("BUY") && request.getPrice() * request.getQuantity() > 80000) {// check client account balance
            message = "Insufficient funds";
        } else if (request.getSide().equals("BUY") && (isOverBuyLimit)) { // actual client stocks
            // Selling
            // check if client owns product // to be obtained from db
            message = "You can not buy more than "+marketData.getBUY_LIMIT();

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
                        isValid,
                        orderRequest.getStrategy()));

        Acknowledgement acknowledgement = new Acknowledgement();
        acknowledgement.setComment(message);
        acknowledgement.setIsValid(isValid);
        acknowledgement.setOrderId(newOrder.getId());

        //report trade activity.
        orderPublisher.publish(newOrder);
        return acknowledgement;
    }
}
