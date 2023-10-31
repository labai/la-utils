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
class NumToWordsLtTest {

    List<NumWord> testNumbers = Arrays.asList(
        NumWord.of(0, "nulis"),
        NumWord.of(1, "vienas"),
        NumWord.of(2, "du"),
        NumWord.of(10, "dešimt"),
        NumWord.of(11, "vienuolika"),
        NumWord.of(20, "dvidešimt"),
        NumWord.of(21, "dvidešimt vienas"),
        NumWord.of(100, "šimtas"),
        NumWord.of(101, "šimtas vienas"),
        NumWord.of(111, "šimtas vienuolika"),
        NumWord.of(200, "du šimtai"),
        NumWord.of(201, "du šimtai vienas"),
        NumWord.of(211, "du šimtai vienuolika"),
        NumWord.of(222, "du šimtai dvidešimt du"),
        NumWord.of(1000, "tūkstantis"),
        NumWord.of(1001, "tūkstantis vienas"),
        NumWord.of(1010, "tūkstantis dešimt"),
        NumWord.of(1011, "tūkstantis vienuolika"),
        NumWord.of(1222, "tūkstantis du šimtai dvidešimt du"),
        NumWord.of(2222, "du tūkstančiai du šimtai dvidešimt du"),
        NumWord.of(10001, "dešimt tūkstančių vienas"),
        NumWord.of(11111, "vienuolika tūkstančių šimtas vienuolika"),
        NumWord.of(100000, "šimtas tūkstančių"),
        NumWord.of(100001, "šimtas tūkstančių vienas"),
        NumWord.of(1_000_001, "vienas milijonas vienas"),
        NumWord.of(1_000_101, "vienas milijonas šimtas vienas"),
        NumWord.of(1_001_101, "vienas milijonas tūkstantis šimtas vienas"),
        NumWord.of(1_002_101, "vienas milijonas du tūkstančiai šimtas vienas"),
        NumWord.of(-1_002_101, "minus vienas milijonas du tūkstančiai šimtas vienas")
    );

    @Test
    void testNumberToWord() {
        for (NumWord pair : testNumbers) {
            assertEquals(pair.words, NumToWordsLt.numberToWords(pair.num.longValue()));
        }
    }

    @Test
    void testNumberToWord1() {
        NumWord pair = NumWord.of(1_000_001, "vienas milijonas vienas");
        assertEquals(pair.words, NumToWordsLt.numberToWords(pair.num.longValue()));
    }
}
