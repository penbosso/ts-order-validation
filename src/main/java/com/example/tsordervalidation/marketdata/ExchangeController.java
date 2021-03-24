package com.example.tsordervalidation.marketdata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ExchangeController {

    @Autowired
    private ExchangeDao exchangeData;

    @GetMapping("/exchanges")
    public List<Exchange> allExchanges() {
        return exchangeData.getExchangeList();
    }
}
