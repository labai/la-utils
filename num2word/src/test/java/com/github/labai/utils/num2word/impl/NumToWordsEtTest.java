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
class NumToWordsEtTest {

    List<NumWord> testNumbers = Arrays.asList(
        NumWord.of(0, "null"),
        NumWord.of(1, "üks"),
        NumWord.of(2, "kaks"),
        NumWord.of(10, "kümme"),
        NumWord.of(11, "üksteist"),
        NumWord.of(20, "kakskümmend"),
        NumWord.of(21, "kakskümmend üks"),
        NumWord.of(100, "ükssada"),
        NumWord.of(101, "ükssada üks"),
        NumWord.of(111, "ükssada üksteist"),
        NumWord.of(200, "kakssada"),
        NumWord.of(201, "kakssada üks"),
        NumWord.of(211, "kakssada üksteist"),
        NumWord.of(222, "kakssada kakskümmend kaks"),
        NumWord.of(1000, "üks tuhat"),
        NumWord.of(1001, "üks tuhat üks"),
        NumWord.of(1010, "üks tuhat kümme"),
        NumWord.of(1011, "üks tuhat üksteist"),
        NumWord.of(1222, "üks tuhat kakssada kakskümmend kaks"),
        NumWord.of(2222, "kaks tuhat kakssada kakskümmend kaks"),
        NumWord.of(10001, "kümme tuhat üks"),
        NumWord.of(11111, "üksteist tuhat ükssada üksteist"),
        NumWord.of(100000, "ükssada tuhat"),
        NumWord.of(100001, "ükssada tuhat üks"),
        NumWord.of(1_000_001, "üks miljon üks"),
        NumWord.of(1_000_101, "üks miljon ükssada üks"),
        NumWord.of(1_001_101, "üks miljon üks tuhat ükssada üks"),
        NumWord.of(1_002_101, "üks miljon kaks tuhat ükssada üks"),
        NumWord.of(-1_002_101, "miinus üks miljon kaks tuhat ükssada üks")
    );

    @Test
    void testNumberToWord() {
        for (NumWord pair : testNumbers) {
            assertEquals(pair.words, NumToWordsEt.numberToWords(pair.num.longValue()));
        }
    }
}
