package com.github.labai.utils.num2word.impl;

import com.github.labai.utils.num2word.impl.TestStruct.NumWord;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Augustus
 * created on 2023.10.28
 */
class NumToWordsEnTest {

    List<NumWord> testNumbers = Arrays.asList(
        NumWord.of(0, "zero"),
        NumWord.of(1, "one"),
        NumWord.of(2, "two"),
        NumWord.of(10, "ten"),
        NumWord.of(11, "eleven"),
        NumWord.of(20, "twenty"),
        NumWord.of(21, "twenty one"),
        NumWord.of(100, "one hundred"),
        NumWord.of(101, "one hundred one"),
        NumWord.of(111, "one hundred eleven"),
        NumWord.of(200, "two hundred"),
        NumWord.of(201, "two hundred one"),
        NumWord.of(211, "two hundred eleven"),
        NumWord.of(222, "two hundred twenty two"),
        NumWord.of(1000, "one thousand"),
        NumWord.of(1001, "one thousand one"),
        NumWord.of(1010, "one thousand ten"),
        NumWord.of(1011, "one thousand eleven"),
        NumWord.of(1222, "one thousand two hundred twenty two"),
        NumWord.of(2222, "two thousand two hundred twenty two"),
        NumWord.of(10001, "ten thousand one"),
        NumWord.of(11111, "eleven thousand one hundred eleven"),
        NumWord.of(100000, "one hundred thousand"),
        NumWord.of(100001, "one hundred thousand one"),
        NumWord.of(1_000_001, "one million one"),
        NumWord.of(1_000_101, "one million one hundred one"),
        NumWord.of(1_001_101, "one million one thousand one hundred one"),
        NumWord.of(1_002_101, "one million two thousand one hundred one"),
        NumWord.of(-1_002_101, "minus one million two thousand one hundred one")
    );

    @Test
    void testNumberToWord() {
        for (NumWord pair : testNumbers) {
            assertEquals(pair.words, NumToWordsEn.numberToWords(pair.num.longValue()));
        }
    }
}
