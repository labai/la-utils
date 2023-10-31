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
class NumToWordsLvTest {

    List<NumWord> testNumbers = Arrays.asList(
        NumWord.of(0, "nulle"),
        NumWord.of(1, "viens"),
        NumWord.of(2, "divi"),
        NumWord.of(10, "desmit"),
        NumWord.of(11, "vienpadsmit"),
        NumWord.of(20, "divdesmit"),
        NumWord.of(21, "divdesmit viens"),
        NumWord.of(100, "simts"),
        NumWord.of(101, "simtu viens"),
        NumWord.of(111, "simts vienpadsmit"),
        NumWord.of(200, "divi simti"),
        NumWord.of(201, "divi simti viens"),
        NumWord.of(211, "divi simti vienpadsmit"),
        NumWord.of(222, "divi simti divdesmit divi"),
        NumWord.of(1000, "viens tūkstotis"),
        NumWord.of(1001, "viens tūkstotis viens"),
        NumWord.of(1010, "viens tūkstotis desmit"),
        NumWord.of(1011, "viens tūkstotis vienpadsmit"),
        NumWord.of(1222, "viens tūkstotis divi simti divdesmit divi"),
        NumWord.of(2222, "divi tūkstoši divi simti divdesmit divi"),
        NumWord.of(10001, "desmit tūkstoši viens"),
        NumWord.of(11111, "vienpadsmit tūkstoši simts vienpadsmit"),
        NumWord.of(100000, "simts tūkstoši"),
        NumWord.of(100001, "simts tūkstoši viens"),
        NumWord.of(1_000_001, "viens miljons viens"),
        NumWord.of(1_000_101, "viens miljons simtu viens"),
        NumWord.of(1_001_101, "viens miljons viens tūkstotis simtu viens"),
        NumWord.of(1_002_101, "viens miljons divi tūkstoši simtu viens"),
        NumWord.of(-1_002_101, "mīnus viens miljons divi tūkstoši simtu viens")
    );

    @Test
    void testNumberToWord() {
        for (NumWord pair : testNumbers) {
            assertEquals(pair.words, NumToWordsLv.numberToWords(pair.num.longValue()));
        }
    }
}
