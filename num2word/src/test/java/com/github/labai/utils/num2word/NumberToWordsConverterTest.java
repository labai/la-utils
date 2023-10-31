package com.github.labai.utils.num2word;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Augustus
 * created on 2023.10.29
 */
class NumberToWordsConverterTest {
    @Test
    void testAmountToWords() {
        Assertions.assertEquals("Šimtas vienas .75", NumberToWordsConverter.amountToWords(new BigDecimal("101.75"), "lt"));
        assertEquals("Nulis .25", NumberToWordsConverter.amountToWords(new BigDecimal("0.25"), "lt"));
        assertEquals("Nulis .25", NumberToWordsConverter.amountToWords(new BigDecimal("0.251"), "lt"));
    }
}
