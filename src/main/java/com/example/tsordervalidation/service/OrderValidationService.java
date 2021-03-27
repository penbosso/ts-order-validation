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

    private List<MarketData> getMarketDataList(String exchangeDataUrl) {

        //access to exchange list in exchange dow
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<List<MarketData>> responseEntity =
                restTemplate.exchange(
                        exchangeDataUrl,
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


        //Todo: to be continued working with all exchanges

        // get market data
        List<MarketData> MarketDataList = getMarketDataList(exchangeData.getExchangeList().get(0).getUrl() + "/md");


        MarketData marketData = null;
        for (MarketData md : MarketDataList) {
            if (md.getTICKER().equals(request.getProduct())) {
                marketData = md;
                break;
            }
        }
        if (marketData == null) {
            return setAckValidity(request, false, "The product does not exist in the market");
        }
        log.info(marketData.toString());


        // check if buy price is reasonable
        if (request.getPrice() > marketData.getMAX_PRICE_SHIFT() + marketData.getMAX_PRICE_SHIFT()) {
            message = request.getPrice() + " is too high for the current market values for this product";

        } else if (request.getPrice() < marketData.getMAX_PRICE_SHIFT() + marketData.getMAX_PRICE_SHIFT()) {
            message = request.getPrice() + " is too low for the current market values for this product";

        } else if (request.getSide() == "SELL" && (request.getProduct() == "IBM")) {
            // Selling
            // check if client owns product // to be obtained from db
            message = "IBM does not exist in your portfolio";

        } else if (request.getSide() == "BUY" && request.getPrice() * request.getQuantity() > 6000) {// check client account balance
            message = "Insufficient funds";
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

        // TODO://report trade activity
        //reporting service OrderAcitvity(orderId, action, status, dataTime)

        //ToDo://proceed to trade engine if valid.
        orderPublisher.publish(newOrder);
        return acknowledgement;
    }
}
