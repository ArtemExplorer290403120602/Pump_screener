package org.example.pump_screener.adapters;

import org.example.pump_screener.adapters.binance.PriceAlertEvent;

import java.math.BigDecimal;

public class PriceAlertEventImpl implements PriceAlertEvent {
    private final String symbol;
    private final BigDecimal priceChange;
    private final BigDecimal volume;

    public PriceAlertEventImpl(String symbol, BigDecimal priceChange, BigDecimal volume) {
        this.symbol = symbol;
        this.priceChange = priceChange;
        this.volume = volume;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public BigDecimal getPriceChange() {
        return priceChange;
    }

    @Override
    public BigDecimal getVolume() {
        return volume;
    }
}
