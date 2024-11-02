package org.example.pump_screener.adapters.binance;

import java.util.List;

public interface BinanceAPI {
    String getAccountStatus();
    List<String> getAllBitcoinPairs();
    List<String>  getAllUsdtPairs();
}