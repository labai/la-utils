package com.github.labai.utils.num2word.impl;

import com.github.labai.utils.num2word.impl.Utils.Triplet;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Augustus
 * created on 2023.10.29
 */
class UtilsTest {

    @Test
    void testGroupThousands() {
        assertTriplet("0:0:0", 0);
        assertTriplet("0:0:1", 1);
        assertTriplet("0:1:0", 10);
        assertTriplet("1:1:1", 111);
        assertTriplet("0:0:1,0:0:0", 1000);
        assertTriplet("0:0:1,0:0:0,0:0:1", 1_000_001);
        assertTriplet("0:0:2,0:0:0,1:1:1", 2_000_111);
    }

    private void assertTriplet(String expected, Number number) {
        List<Triplet> triplets = Utils.groupTriplets(number.longValue());
        String str = triplets.stream().map(Triplet::toString).collect(Collectors.joining(","));
        assertEquals(expected, str);
    }
}
