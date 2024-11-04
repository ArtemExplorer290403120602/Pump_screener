package org.example.pump_screener.adapters.binance;

import org.example.pump_screener.service.BinanceService;

public interface CandlestickEvent {
    String getSymbol();
    BinanceService.Candlestick getCandlestick();
}
