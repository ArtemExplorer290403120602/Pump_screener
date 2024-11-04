package org.example.pump_screener.adapters.binance;

import java.math.BigDecimal;

public interface PriceAlertEvent {
    String getSymbol();
    BigDecimal getPriceChange();
    BigDecimal getVolume();
}
