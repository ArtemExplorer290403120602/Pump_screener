package org.example.pump_screener.graphics;

import org.example.pump_screener.socket.Candlestick;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.*;
import java.util.Date;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class ChartGenerator {

    public static File createCandlestickChart(String symbol, List<Candlestick> candlesticks) {
        int size = candlesticks.size();
        // Create an array of OHLCDataItem
        OHLCDataItem[] items = new OHLCDataItem[size];

        for (int i = 0; i < size; i++) {
            Candlestick candlestick = candlesticks.get(i);
            long openTime = candlestick.getOpenTime();
            double open = Double.parseDouble(candlestick.getOpen());
            double high = Double.parseDouble(candlestick.getHigh());
            double low = Double.parseDouble(candlestick.getLow());
            double close = Double.parseDouble(candlestick.getClose());

            // Creating OHLCDataItem for each candlestick
            // Пятый аргумент — это значение объемов, используем 0 как заглушку
            double volume = 0.0;
            items[i] = new OHLCDataItem(new Date(openTime), open, high, low, close, volume);
        }

        // Create the dataset using the array of OHLCDataItem
        OHLCDataset dataset = new DefaultOHLCDataset(symbol, items);

        // Creating the chart
        JFreeChart chart = ChartFactory.createCandlestickChart(
                "Candlestick Chart for " + symbol,
                "Time",
                "Price",
                dataset,
                false
        );

        File chartFile = new File(symbol + "_chart.png");
        try {
            ChartUtils.saveChartAsPNG(chartFile, chart, 800, 600);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return chartFile;
    }
}
