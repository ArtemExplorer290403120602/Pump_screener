package org.example.pump_screener.adapters;

import org.example.pump_screener.adapters.binance.CandlestickEvent;
import org.example.pump_screener.service.BinanceService;

public class CandlestickEventImpl implements CandlestickEvent {
    private final String symbol;
    private final BinanceService.Candlestick candlestick;

    public CandlestickEventImpl(String symbol, BinanceService.Candlestick candlestick) {
        this.symbol = symbol;
        this.candlestick = candlestick;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public BinanceService.Candlestick getCandlestick() {
        return candlestick;
    }
}
