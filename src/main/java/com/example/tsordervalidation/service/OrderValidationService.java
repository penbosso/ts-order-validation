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
import org.springframework.ws.server.endpoint.annotation.RequestPayload;

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

    private List<MarketData> getMarketDataList(String exchangeDataUrl){

        //access to exchange list in exchange dow
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<List<MarketData>> responseEntity =
                restTemplate.exchange(
                        exchangeDataUrl,
                        HttpMethod.GET, null,
                        new ParameterizedTypeReference<List<MarketData>>() {}
                );
        List<MarketData> MarketDataList = responseEntity.getBody();
        return MarketDataList.stream().collect(Collectors.toList());
    }

    public Acknowledgement checkOrderValidity(OrderRequest request) {
        //starter list, make get request when the app start, update the list when the sub data comes
        //container[ex1,ex2,exy]

        boolean isValid = false;
        String message = "";


        //Todo: to be continued working with all exchanges

        // get market data
        List<MarketData> MarketDataList = getMarketDataList(exchangeData.getExchangeList().get(0).getUrl() + "/md");


        MarketData marketData = null;
        for (MarketData md: MarketDataList) {
            if (md.getTICKER().equals(request.getProduct()) ){
                marketData = md;
                break;
            }
        }
        if(marketData == null) {
            return setAckValidity(request, false, "The product does not exist in the market");
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

        } else {// check client account balance
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
        orderPublisher.publish(newOrder);
        return acknowledgement;
    }
}
