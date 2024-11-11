package org.example.pump_screener.adapters.binance;

import org.example.pump_screener.service.BinanceService;
import org.example.pump_screener.socket.Candlestick;

public interface CandlestickEvent {
    String getSymbol();
    Candlestick getCandlestick();
}
