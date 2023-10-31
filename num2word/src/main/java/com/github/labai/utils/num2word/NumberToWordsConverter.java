/*
The MIT License (MIT)

Copyright (c) 2023 Augustus

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.github.labai.utils.num2word;

import com.github.labai.utils.num2word.impl.NumToWordsEn;
import com.github.labai.utils.num2word.impl.NumToWordsEt;
import com.github.labai.utils.num2word.impl.NumToWordsLt;
import com.github.labai.utils.num2word.impl.NumToWordsLv;
import com.github.labai.utils.num2word.impl.NumToWordsRu;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Augustus, 2023.10.29
 * convert number to words
 * 123 -> one hundred twenty three
 */
public class NumberToWordsConverter {

    public static String numberToWords(long longNum, String lang) {
        switch (lang) {
            case "en":
                return NumToWordsEn.numberToWords(longNum);
            case "lt":
                return NumToWordsLt.numberToWords(longNum);
            case "lv":
                return NumToWordsLv.numberToWords(longNum);
            case "et":
                return NumToWordsEt.numberToWords(longNum);
            case "ru":
                return NumToWordsRu.numberToWords(longNum);
            default:
                throw new IllegalArgumentException("Invalid language code '" + lang + "'");
        }
    }

    public static String amountToWords(BigDecimal amount, String lang) {
        String cents = amount
            .subtract(amount.setScale(0, RoundingMode.DOWN))
            .multiply(new BigDecimal(100))
            .setScale(0, RoundingMode.HALF_UP)
            .toString();
        String words = numberToWords(amount.longValue(), lang);
        if (!words.isEmpty())
            words = words.substring(0, 1).toUpperCase() + words.substring(1);
        return words + " ." + cents;
    }
}
