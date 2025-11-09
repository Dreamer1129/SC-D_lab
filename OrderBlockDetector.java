package com.example.cryptoscannerbackend.service;

import com.example.cryptoscannerbackend.model.OrderBlockResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class OrderBlockDetector {

    // This is a conceptual and simplified advanced order block detection logic.
    // A real-world implementation would be much more complex, involving:
    // - Identifying "imbalance" or "fair value gaps"
    // - Looking for strong impulsive moves
    // - Confirmation with volume analysis
    // - Consideration of market structure (breaks of structure, changes of character)
    // - Multiple candlestick patterns (e.g., engulfing candles, strong momentum candles)

    public OrderBlockResult detectOrderBlock(com.example.cryptoscannerbackend.model.CoinData.CoinData coin, List<BinanceApiClient.Candlestick> klines) {
        OrderBlockResult result = new OrderBlockResult();
        result.setId(coin.getId());
        result.setName(coin.getName());
        result.setCurrentPrice(coin.getCurrentPrice());
        result.setVolume(coin.getVolume());
        result.setTimestamp(LocalDateTime.now().atZone(ZoneId.systemDefault()).toLocalTime().toString());

        if (klines == null || klines.size() < 3) { // Need at least a few candles to analyze
            result.setOrderBlockType("None");
            result.setDetails("Not enough candlestick data for analysis.");
            return result;
        }

        // Simplified logic for demonstration:
        // Look for a strong bearish candle followed by a strong bullish candle (potential buying OB)
        // Or a strong bullish candle followed by a strong bearish candle (potential selling OB)
        // And check for significant volume on the "order block" candle itself.

        // Let's consider the last 3 candles for a very basic example
        // klines[klines.size() - 1] is the current/latest candle
        // klines[klines.size() - 2] is the previous candle
        // klines[klines.size() - 3] is the candle before that

        BinanceApiClient.Candlestick latestCandle = klines.get(klines.size() - 1);
        BinanceApiClient.Candlestick prevCandle = klines.get(klines.size() - 2);
        BinanceApiClient.Candlestick twoPrevCandle = klines.get(klines.size() - 3);

        double currentPrice = coin.getCurrentPrice();

        // Define what constitutes a "strong" candle (e.g., large body relative to range)
        // And "significant volume" (e.g., above average volume for recent candles)
        double averageVolume = klines.stream().mapToDouble(Candlestick::getVolume).average().orElse(0.0);
        double strongCandleThreshold = 0.01; // 1% body size relative to price
        double significantVolumeThreshold = 1.5; // 1.5 times average volume

        // Check for Buying Order Block (Bullish OB)
        // Pattern: Strong bearish candle, followed by a strong bullish candle (or a reversal candle)
        // and the previous bearish candle's low is not broken by the current price.
        if (prevCandle.getClose() < prevCandle.getOpen() && // Previous candle is bearish
                (prevCandle.getOpen() - prevCandle.getClose()) / prevCandle.getOpen() > strongCandleThreshold && // Strong bearish
                prevCandle.getVolume() > averageVolume * significantVolumeThreshold && // High volume on bearish candle
                currentPrice > prevCandle.getLow() && // Current price is above previous candle's low
                (latestCandle.getClose() > latestCandle.getOpen() || // Latest is bullish
                        (latestCandle.getOpen() - latestCandle.getClose()) / latestCandle.getOpen() < strongCandleThreshold) // Or latest is small/reversal
        ) {
            // A potential buying order block could be around the open of the strong bearish candle,
            // or the midpoint of its body, or the high of the candle before it.
            result.setOrderBlockType("Buying (Bullish)");
            // A common approach is to use the open of the bearish candle that caused the imbalance
            result.setOrderBlockPrice(prevCandle.getOpen());
            result.setDetails(
                    String.format("Potential 4H Buying Order Block detected near $%.2f. " +
                                    "Identified by a strong bearish candle with high volume, indicating potential demand zone.",
                            result.getOrderBlockPrice())
            );
            return result;
        }

        // Check for Selling Order Block (Bearish OB)
        // Pattern: Strong bullish candle, followed by a strong bearish candle (or a reversal candle)
        // and the previous bullish candle's high is not broken by the current price.
        if (prevCandle.getClose() > prevCandle.getOpen() && // Previous candle is bullish
                (prevCandle.getClose() - prevCandle.getOpen()) / prevCandle.getOpen() > strongCandleThreshold && // Strong bullish
                prevCandle.getVolume() > averageVolume * significantVolumeThreshold && // High volume on bullish candle
                currentPrice < prevCandle.getHigh() && // Current price is below previous candle's high
                (latestCandle.getClose() < latestCandle.getOpen() || // Latest is bearish
                        (latestCandle.getClose() - latestCandle.getOpen()) / latestCandle.getOpen() < strongCandleThreshold) // Or latest is small/reversal
        ) {
            // A potential selling order block could be around the open of the strong bullish candle,
            // or the midpoint of its body, or the low of the candle before it.
            result.setOrderBlockType("Selling (Bearish)");
            // A common approach is to use the open of the bullish candle that caused the imbalance
            result.setOrderBlockPrice(prevCandle.getOpen());
            result.setDetails(
                    String.format("Potential 4H Selling Order Block detected near $%.2f. " +
                                    "Identified by a strong bullish candle with high volume, indicating potential supply zone.",
                            result.getOrderBlockPrice())
            );
            return result;
        }

        result.setOrderBlockType("None");
        result.setDetails("No significant 4H order block detected based on current logic.");
        return result;
    }
}
