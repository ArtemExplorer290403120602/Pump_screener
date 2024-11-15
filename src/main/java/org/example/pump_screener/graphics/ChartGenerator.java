package org.example.pump_screener.graphics;

import org.example.pump_screener.socket.Candlestick;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.data.xy.DefaultOHLCDataset;
import org.jfree.data.xy.OHLCDataItem;
import org.jfree.data.xy.OHLCDataset;

import java.awt.*;
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

        // Установка параметров оси Y для улучшения читабельности
        XYPlot plot = chart.getXYPlot();
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setAutoRangeIncludesZero(false); // Убираем ноль из диапазона
        yAxis.setLowerBound(getLowerBound(candlesticks)); // Устанавливаем нижнюю границу
        yAxis.setUpperBound(getUpperBound(candlesticks)); // Устанавливаем верхнюю границу

        // Установка темного фона графика
        chart.setBackgroundPaint(Color.DARK_GRAY);
        plot.setBackgroundPaint(Color.BLACK);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // Установка цвета для осей и меток
        yAxis.setLabelPaint(Color.LIGHT_GRAY);
        yAxis.setTickLabelPaint(Color.LIGHT_GRAY);
        plot.getDomainAxis().setLabelPaint(Color.LIGHT_GRAY);
        plot.getDomainAxis().setTickLabelPaint(Color.LIGHT_GRAY);

        // Установка размеров графика
        File chartFile = new File(symbol + "_chart.png");
        try {
            ChartUtils.saveChartAsPNG(chartFile, chart, 800, 600); // Ширина и высота графика
        } catch (IOException e) {
            e.printStackTrace();
        }
        return chartFile;
    }

    // Метод для вычисления нижней границы ценового диапазона
    private static double getLowerBound(List<Candlestick> candlesticks) {
        return candlesticks.stream()
                .mapToDouble(c -> Double.parseDouble(c.getLow()))
                .min()
                .orElse(0);
    }

    // Метод для вычисления верхней границы ценового диапазона
    private static double getUpperBound(List<Candlestick> candlesticks) {
        return candlesticks.stream()
                .mapToDouble(c -> Double.parseDouble(c.getHigh()))
                .max()
                .orElse(0);
    }
}