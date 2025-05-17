package jtest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Augustus
 * created on 2022.11.21
 */
public class StructuresInJava {

    public static class Test1Pojo {
        public String field1;
        private String field2;
        private String prop1 = "prop1";
        private String prop2;
        public String prop3;
        @NotNull private String prop4 = "prop4";
        @Nullable private String prop5;
        private String wrong1x;
        private String wrong2;
        private String prop6 = "prop6";
        private String prop7 = "prop7";
        private String prop8 = "prop8";
        private String prop9 = "prop9";

        private Boolean prop10 = Boolean.FALSE;
        private boolean prop11 = false;
        private String prop12 = "false";

        public String getProp1() { return prop1; }
        public String getProp2() { return prop2; }
        public void setProp2(String value) { prop2 = value; }
        public String getProp3() { return prop3; }
        public void setProp3(String value) { prop3 = value; }

        @NotNull public String getProp4() {return prop4;}
        public void setProp4(@NotNull String prop4) {this.prop4 = prop4;}
        @Nullable public String getProp5() {return prop5;}
        public void setProp5(@Nullable String prop5) {this.prop5 = prop5;}

        // wrong getter (mismatch field and function names)
        public String getWrong1() {return wrong1x;}
        public void setWrong1(String wrong1) {this.wrong1x = wrong1; }

        // wrong getter (mismatch field and function types)
        @Nullable public Integer getWrong2() {return wrong2 == null ? null : Integer.parseInt(wrong2);}
        public void setWrong2(Integer wrong2) {this.wrong2 = wrong2 == null ? null : String.valueOf(wrong2);}

        // not setters
        public String retrieveField2() { return field2; }
        public void assignField2(String value) { field2 = value; }

        // wrong - static
        public static String getProp6() { return "static_prop6"; }
        public static void setProp6(String prop6) { }

        // wrong - method with param
        public String getProp7(String s) { return prop7; }
        public void getProp7(String s, String s2) { prop7 = s; }

        // ok - w/o "get" prefix
        public String prop8() { return prop8; }
        public void prop8(String prop8) { this.prop8 = prop8; }

        // 2 getters
        public String getProp9() { return "get-" + prop9; }
        public void setProp9(String prop9) {this.prop9 = prop9;}
        public String prop9() { return "noget-" + prop9; }
        public void prop9(String prop9) {this.prop9 = prop9;}

        // "is" prefix
        public Boolean isProp10() {return prop10;}
        public boolean isProp11() {return prop11;}
        public String isProp12() {return prop12;}
    }

    public static class Test2PojoConstr {
        private final String prop1;

        public Test2PojoConstr(String prop1) {
            this.prop1 = prop1;
        }

        public String getProp1() { return prop1; }
    }

    public record Record12(
        Long v01,
        long v02,
        Integer v03,
        int v04,
        String v06
    ) {
    }

}
