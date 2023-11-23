import com.github.labai.utils.mapper.LaMapperJ;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Augustus
 *         created on 2023.11.22
 */
public class LaMapperJTest {

    public static class Dto1 {
        public String f01;
        private Integer f02;

        public Integer getF02() {
            return f02;
        }
        public void setF02(Integer f02) {
            this.f02 = f02;
        }
    }

    @Test
    void testLaMapperJ() {
        Dto1 fr = new Dto1();
        fr.f01 = "a";
        fr.f02 = 5;
        Dto1 to = LaMapperJ.copyFrom(fr, Dto1.class);

        assertEquals("a", to.f01);
        assertEquals(5, to.f02);
    }
}
