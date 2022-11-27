/**
 * @author Augustus
 * created on 2022.11.21
 */
public class TestStructJv {


    public static class Test1Pojo {
        public String field1;
        private String field2;
        private String prop1 = "prop1";
        private String prop2;
        public String prop3;

        public String getProp1() { return prop1; }
        public String getProp2() { return prop2; }
        public void setProp2(String value) { prop2 = value; }
        public String getProp3() { return prop3; }
        public void setProp3(String value) { prop3 = value; }

        // not setters
        public String retrieveField2() { return field2; }
        public void assignField2(String value) { field2 = value; }

    }

    static class Test2PojoConstr {
        private final String prop1;

        public Test2PojoConstr(String prop1) {
            this.prop1 = prop1;
        }

        public String getProp1() { return prop1; }
    }

}
