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
 * Augustus, 2023.10.30
 * number to words - Russian version
 */
public class NumToWordsRu {
    private static final String MINUS = "минус";
    private static final String ZERO = "нуль";
    private static final String ONE_FOR_THOUSAND = "одна";
    private static final String TWO_FOR_THOUSAND = "две";

    private static final String[] ONES = {
        "",
        "один",
        "два",
        "три",
        "четыре",
        "пять",
        "шесть",
        "семь",
        "восемь",
        "девять"
    };

    private static final String[] TEENS = {
        "десять",
        "одиннадцать",
        "двенадцать",
        "тринадцать",
        "четырнадцать",
        "пятнадцать",
        "шестнадцать",
        "семнадцать",
        "восемнадцать",
        "девятнадцать"
    };


    private static final String[] TENS = {
        "",
        "десять",
        "двадцать",
        "тридцать",
        "сорок",
        "пятьдесят",
        "шестьдесят",
        "семьдесят",
        "восемьдесят",
        "девяносто"
    };

    private static final String[] HUNDREDS = {
        "",
        "сто",
        "двести",
        "триста",
        "четыреста",
        "пятьсот",
        "шестьсот",
        "семьсот",
        "восемьсот",
        "девятьсот"
    };

    private static final String[][] TRIPLETS = {
        {},
        {"тысяча", "тысячи", "тысяч"},
        {"миллион", "миллиона", "миллионов"},
        {"миллиард", "миллиарда", "миллиардов"},
        {"триллион", "триллиона", "триллионов"},
        {"квадриллион", "квадриллиона", "квадриллионов"}
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
                String[] tripletNames = TRIPLETS[tri.tripletPos];
                if (tri.tend == 1) {
                    words.add(getEnding(10 + tri.oned, tripletNames));
                } else {
                    words.add(getEnding(tri.oned, tripletNames));
                }
            }
        }

        return String.join(" ", words);
    }

    private static List<String> subThousandInWords(Triplet g) {
        List<String> words = new ArrayList<>();

        if (g.value == 0) {
            return words;
        }

        if (g.hund != 0) {
            words.add(HUNDREDS[g.hund]);
        }

        if (g.tend == 1) {
            words.add(TEENS[g.oned]);
            return words;
        }

        if (g.tend != 0) {
            words.add(TENS[g.tend]);
        }

        if (g.oned == 0) {
            return words;
        }

        if (g.tripletPos == 1 && g.oned == 1) {
            words.add(ONE_FOR_THOUSAND);
        } else if (g.tripletPos == 1 && g.oned == 2) {
            words.add(TWO_FOR_THOUSAND);
        } else {
            words.add(ONES[g.oned]);
        }

        return words;
    }

    public static String getEnding(int number, String[] tripletForms) {
        int sng = number % 10;
        int ten = number % 100;
        if (ten >= 5 && ten <= 20) {
            return tripletForms[2];
        } else if (sng == 1) {
            return tripletForms[0]; // ..иллион
        } else if (sng >= 2 && sng <= 4) {
            return tripletForms[1]; // ..иллиона
        } else {
            return tripletForms[2]; // ..иллионов
        }
    }
}
