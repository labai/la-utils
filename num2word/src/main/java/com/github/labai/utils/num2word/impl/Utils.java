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

import java.util.Arrays;
import java.util.List;

/**
 * Augustus, 2023.10.29
 * for local usage only
 */
class Utils {

    static class Triplet {
        final int value; // value
        final int tripletPos; // triplet position in number
        final int hund; // hundred's digit
        final int tend; // ten's digit
        final int oned; // one's digit

        public Triplet(int num, int tripletPos) {
            this.tripletPos = tripletPos;
            num = Math.abs(num) % 1000;
            this.value = num;
            this.oned = num % 10;
            num /= 10;
            this.tend = num % 10;
            num /= 10;
            this.hund = num % 10;
        }

        @Override
        public String toString() {
            return hund + ":" + tend + ":" + oned;
        }
    }

    // a thousand groups with 3 sub-thousand digits in each
    // 2_000_111 -> 0:0:2, 0:0:0, 1:1:1
    static List<Triplet> groupTriplets(long number) {
        int numDigits = String.valueOf(number).length();
        int grpNum = (numDigits - 1) / 3 + 1;

        Triplet[] res = new Triplet[grpNum];

        int i = 0;
        while (i < grpNum) {
            int subk = (int) (number % 1000);
            number = number / 1000;
            res[grpNum - i - 1] = new Triplet(subk, i);
            i++;
        }
        return Arrays.asList(res);
    }
}
