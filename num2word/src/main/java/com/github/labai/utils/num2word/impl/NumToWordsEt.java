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
package com.github.labai.utils.num2word.impl;

import com.github.labai.utils.num2word.impl.Utils.Triplet;

import java.util.ArrayList;
import java.util.List;

/**
 * Augustus, 2023.10.29
 * number to words - Estonian version
 */
public class NumToWordsEt {
    private static final String MINUS = "miinus";
    private static final String ZERO = "null";

    private static final String[] ONES = {
        "",
        "üks",
        "kaks",
        "kolm",
        "neli",
        "viis",
        "kuus",
        "seitse",
        "kaheksa",
        "üheksa",
    };

    private static final String TEN = "kümme";
    private static final String TEN_SFX = "kümmend";
    private static final String HUNDRED_SFX = "sada";
    private static final String TEEN_SFX = "teist";
    private static final String IT_SFX = "it";

    private static final String[] TRIPLETS = {
        "",
        "tuhat",
        "miljon",
        "miljard",
        "triljon",
        "kvadriljon",
        "kvintiljon",
        "sekstiljon",
        "septiljon",
        "oktiljon",
        "noniljon",
    };

    public static String numberToWords(long number) {
        if (number == 0) {
            return ZERO;
        }

        List<String> words = new ArrayList<>();
        if (number < 0) {
            number = Math.abs(number);
            words.add(MINUS);
        }

        List<Triplet> triplets = Utils.groupTriplets(number);
        for (Triplet tri : triplets) {
            if (tri.value == 0)
                continue;

            words.addAll(subThousandInWords(tri));

            if (tri.tripletPos > 0) {
                if (tri.value == 1 || tri.tripletPos == 1) {
                    words.add(TRIPLETS[tri.tripletPos]);
                } else {
                    words.add(TRIPLETS[tri.tripletPos] + IT_SFX);
                }
            }
        }

        return String.join(" ", words);
    }

    private static List<String> subThousandInWords(Triplet g) {
        List<String> words = new ArrayList<>();

        if (g.hund > 0) {
            words.add(ONES[g.hund] + HUNDRED_SFX);
        }

        if (g.tend != 0) {
            if (g.tend == 1) {
                if (g.oned == 0) {
                    words.add(TEN);
                } else if (g.oned >= 1 && g.oned <= 9) {
                    words.add(ONES[g.oned] + TEEN_SFX);
                }
            } else {
                words.add(ONES[g.tend] + TEN_SFX);
            }
        }

        if (g.tend != 1 && g.oned > 0) {
            words.add(ONES[g.oned]);
        }

        return words;
    }
}
