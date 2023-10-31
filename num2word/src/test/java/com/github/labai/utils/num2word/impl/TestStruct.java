package com.github.labai.utils.num2word.impl;

/**
 * @author Augustus
 * created on 2023.10.31
 */
class TestStruct {

    static class NumWord {
        final Number num;
        final String words;

        public NumWord(Number num, String words) {
            this.num = num;
            this.words = words;
        }

        public static NumWord of(Number num, String words) {
            return new NumWord(num, words);
        }
    }
}
