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
 * number to words - Latvian version
 */
public class NumToWordsLv {
    private static final String MINUS = "mīnus";
    private static final String ZERO = "nulle";

    private static final String[] ONES = {
        "",
        "viens",
        "divi",
        "trīs",
        "četri",
        "pieci",
        "seši",
        "septiņi",
        "astoņi",
        "deviņi"
    };

    private static final String[] TEENS = {
        "desmit",
        "vienpadsmit",
        "divpadsmit",
        "trīspadsmit",
        "četrpadsmit",
        "piecpadsmit",
        "sešpadsmit",
        "septiņpadsmit",
        "astoņpadsmit",
        "deviņpadsmit"
    };

    private static final String[] TENS = {
        "",
        "desmit",
        "divdesmit",
        "trīsdesmit",
        "četrdesmit",
        "piecdesmit",
        "sešdesmit",
        "septiņdesmit",
        "astoņdesmit",
        "deviņdesmit"
    };

    private static final String[] HUNDRED = {"simts", "simti", "simtu"};

    private static final String[][] TRIPLETS = {
        {},
        {"tūkstotis", "tūkstoši", "tūkstošu"},
        {"miljons", "miljoni", "miljonu"},
        {"miljards", "miljardi", "miljardu"},
        {"triljons", "triljoni", "triljonu"},
        {"kvadriljons", "kvadriljoni", "kvadriljonu"},
        {"kvintiljons", "kvintiljoni", "kvintiljonu"},
        {"sikstiljons", "sikstiljoni", "sikstiljonu"},
        {"septiljons", "septiljoni", "septiljonu"},
        {"oktiljons", "oktiljoni", "oktiljonu"},
        {"nontiljons", "nontiljoni", "nontiljonu"}
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
                words.add(getEnding(tri.value, TRIPLETS[tri.tripletPos]));
            }
        }

        return String.join(" ", words);
    }

    private static List<String> subThousandInWords(Triplet g) {
        List<String> words = new ArrayList<>();

        if (g.hund > 0) {
            if (g.hund == 1 && g.tend == 0 && g.oned > 0) {
                words.add(HUNDRED[2]);
            } else if (g.hund > 1) {
                words.add(ONES[g.hund]);
                words.add(HUNDRED[1]);
            } else {
                words.add(HUNDRED[0]);
            }
        }

        if (g.tend > 1) {
            words.add(TENS[g.tend]);
        }

        if (g.tend == 1) {
            words.add(TEENS[g.oned]);
        } else if (g.oned > 0) {
            words.add(ONES[g.oned]);
        }

        return words;
    }

    private static String getEnding(int n, String[] tripletForms) {
        if (n % 10 == 1 && n % 100 != 11)
            return tripletForms[0];
        if (n == 0)
            return tripletForms[2];
        return tripletForms[1];
    }
}
