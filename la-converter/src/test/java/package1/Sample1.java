package package1;

/**
 * @author Augustus
 * created on 2023.03.03
 */
public class Sample1 {

    void test1() {
        Pojo1 p1 = new Pojo1();
        Pojo2 p2 = new Pojo2(p1.getA01(), p1.getA02(), 1101);
        p2.setA03(p1.getA03());
        p2.setA04(p1.getA04());
    }
}
