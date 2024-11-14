package org.example.pump_screener.adapters;

import org.example.pump_screener.adapters.binance.CandlestickEvent;
import org.example.pump_screener.socket.Candlestick;

public class CandlestickEventImpl implements CandlestickEvent {
    private final String symbol;
    private final Candlestick candlestick;

    public CandlestickEventImpl(String symbol, Candlestick candlestick) {
        this.symbol = symbol;
        this.candlestick = candlestick;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public Candlestick getCandlestick() {
        return candlestick;
    }
}
