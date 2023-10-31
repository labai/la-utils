package com.github.labai.utils.num2word.impl;

import com.github.labai.utils.num2word.impl.TestStruct.NumWord;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Augustus
 * created on 2023.10.30
 */
class NumToWordsRuTest {

    List<NumWord> testNumbers = Arrays.asList(
        NumWord.of(0, "нуль"),
        NumWord.of(1, "один"),
        NumWord.of(2, "два"),
        NumWord.of(10, "десять"),
        NumWord.of(11, "одиннадцать"),
        NumWord.of(20, "двадцать"),
        NumWord.of(21, "двадцать один"),
        NumWord.of(100, "сто"),
        NumWord.of(101, "сто один"),
        NumWord.of(111, "сто одиннадцать"),
        NumWord.of(200, "двести"),
        NumWord.of(201, "двести один"),
        NumWord.of(211, "двести одиннадцать"),
        NumWord.of(222, "двести двадцать два"),
        NumWord.of(1000, "одна тысяча"),
        NumWord.of(1001, "одна тысяча один"),
        NumWord.of(1010, "одна тысяча десять"),
        NumWord.of(1011, "одна тысяча одиннадцать"),
        NumWord.of(1222, "одна тысяча двести двадцать два"),
        NumWord.of(2222, "две тысячи двести двадцать два"),
        NumWord.of(5001, "пять тысяч один"),
        NumWord.of(10001, "десять тысяч один"),
        NumWord.of(11111, "одиннадцать тысяч сто одиннадцать"),
        NumWord.of(100000, "сто тысяч"),
        NumWord.of(100001, "сто тысяч один"),
        NumWord.of(101001, "сто одна тысяча один"),
        NumWord.of(1000001, "один миллион один"),
        NumWord.of(1000101, "один миллион сто один"),
        NumWord.of(1001101, "один миллион одна тысяча сто один"),
        NumWord.of(1002101, "один миллион две тысячи сто один"),
        NumWord.of(2000001, "два миллиона один"),
        NumWord.of(5000001, "пять миллионов один"),
        NumWord.of(-1002101, "минус один миллион две тысячи сто один")
    );

    @Test
    void testNumberToWord() {
        for (NumWord pair : testNumbers) {
            assertEquals(pair.words, NumToWordsRu.numberToWords(pair.num.longValue()));
        }
    }
}
