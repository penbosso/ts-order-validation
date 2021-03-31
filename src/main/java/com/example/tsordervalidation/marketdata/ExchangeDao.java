package com.example.tsordervalidation.marketdata;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ExchangeDao {
    public static List<Exchange> exchangeList = new ArrayList<Exchange>();
    private static int listCount = 2;

    static {
        exchangeList.add(new Exchange(1,"https://exchange.matraining.com",true));
        exchangeList.add(new Exchange(2,"https://exchange2.matraining.com",true));
    }

    public List<Exchange> getExchangeList() {
        return exchangeList;
    }

    public Exchange save(Exchange exchange) {
        if(exchange.getId()==null) {
            exchange.setId(++listCount);
        }
        exchangeList.add(exchange);
        return exchange;
    }
    public void toggleIsEnable(int id) {
        exchangeList = (List<Exchange>) exchangeList.stream().map(exchange -> {
            if(exchange.getId() == id) {
                exchange.setEnable(!exchange.isEnable());
            }
            return exchange;
        });
    }

    public void delete(int id) {
        exchangeList = (List<Exchange>) exchangeList.stream().filter(exchange -> {
            if(exchange.getId() == id) {
                return false;
            }
            return true;
        });
    }
}
