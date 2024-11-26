package org.example.pump_screener.socket;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@Component
public class BollingerBands {
    private BigDecimal sma;
    private BigDecimal upperBand;
    private BigDecimal lowerBand;

    public BollingerBands(BigDecimal sma, BigDecimal upperBand, BigDecimal lowerBand) {
        this.sma = sma;
        this.upperBand = upperBand;
        this.lowerBand = lowerBand;
    }

    public BigDecimal getSma() {
        return sma;
    }

    public BigDecimal getUpperBand() {
        return upperBand;
    }

    public BigDecimal getLowerBand() {
        return lowerBand;
    }
}
