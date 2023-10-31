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
 * number to words - Lithuanian version
 */
public class NumToWordsLt {
    private static final String MINUS = "minus";
    private static final String ZERO = "nulis";

    private static final String[] ONES = {
        "",
        "vienas",
        "du",
        "trys",
        "keturi",
        "penki",
        "šeši",
        "septyni",
        "aštuoni",
        "devyni"
    };

    private static final String[] TEENS = {
        "dešimt",
        "vienuolika",
        "dvylika",
        "trylika",
        "keturiolika",
        "penkiolika",
        "šešiolika",
        "septyniolika",
        "aštuoniolika",
        "devyniolika"
    };

    private static final String[] TENS = {
        "",
        "dešimt",
        "dvidešimt",
        "trisdešimt",
        "keturiasdešimt",
        "penkiasdešimt",
        "šešiasdešimt",
        "septyniasdešimt",
        "aštuoniasdešimt",
        "devyniasdešimt"
    };

    private static final String[] HUNDRED = {"šimtas", "šimtai"};

    private static final String[][] TRIPLETS = {
        {},
        {"tūkstantis", "tūkstančiai", "tūkstančių"},
        {"milijonas", "milijonai", "milijonų"},
        {"milijardas", "milijardai", "milijardų"},
        {"trilijonas", "trilijonai", "trilijonų"},
        {"kvadrilijonas", "kvadrilijonai", "kvadrilijonų"},
        {"kvintilijonas", "kvintilijonai", "kvintilijonų"},
        {"sikstilijonas", "sikstilijonai", "sikstilijonų"},
        {"septilijonas", "septilijonai", "septilijonų"},
        {"oktilijonas", "oktilijonai", "oktilijonų"},
        {"naintilijonas", "naintilijonai", "naintilijonų"},
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

            if (!(tri.tripletPos == 1 && tri.value == 1)) { // do not write 'vienas' before a thousand (?)
                words.addAll(subThousandInWords(tri));
            }

            if (tri.tripletPos > 0) {
                words.add(getEnding(tri, TRIPLETS[tri.tripletPos]));
            }
        }

        return String.join(" ", words);
    }

    private static List<String> subThousandInWords(Triplet g) {
        List<String> words = new ArrayList<>();

        if (g.value == 0)
            return words;

        if (g.hund > 0) {
            if (g.hund > 1) {
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

    private static String getEnding(Triplet g, String[] tripletForms) {
        if (g.tend == 1 || g.oned == 0)
            return tripletForms[2]; // ..iu
        else if (g.oned == 1)
            return tripletForms[0]; // ..is
        else
            return tripletForms[1]; // ..iai
    }
}
