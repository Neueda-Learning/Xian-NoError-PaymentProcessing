package com.example.paymentprocessing.service;

import com.example.paymentprocessing.enums.CurrencyCode;
import com.example.paymentprocessing.enums.PaymentErrorCode;
import com.example.paymentprocessing.exception.PaymentValidationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class CurrencyConversionService {

    private static final int SCALE = 2;

    private static final Map<CurrencyCode, BigDecimal> RATES_TO_USD = Map.of(
            CurrencyCode.USD, BigDecimal.ONE,
            CurrencyCode.CNY, new BigDecimal("0.14"),
            CurrencyCode.EUR, new BigDecimal("1.09"),
            CurrencyCode.GBP, new BigDecimal("1.27")
    );

    public BigDecimal convert(BigDecimal amount, CurrencyCode fromCurrency, CurrencyCode toCurrency) {
        if (amount == null) {
            throw new PaymentValidationException(
                    PaymentErrorCode.INVALID_AMOUNT,
                    "Payment amount is required"
            );
        }

        if (fromCurrency == null || toCurrency == null) {
            throw new PaymentValidationException(
                    PaymentErrorCode.INVALID_CURRENCY,
                    "Both source and destination currencies are required for conversion"
            );
        }

        if (fromCurrency == toCurrency) {
            return amount.setScale(SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal fromRate = getRateOrThrow(fromCurrency);
        BigDecimal toRate = getRateOrThrow(toCurrency);

        BigDecimal amountInUsd = amount.multiply(fromRate);
        return amountInUsd
                .divide(toRate, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal getRateOrThrow(CurrencyCode currencyCode) {
        BigDecimal rate = RATES_TO_USD.get(currencyCode);
        if (rate == null) {
            throw new PaymentValidationException(
                    PaymentErrorCode.INVALID_CURRENCY,
                    "Unsupported currency conversion for " + currencyCode
            );
        }
        return rate;
    }
}