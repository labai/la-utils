package package1;

import com.github.labai.utils.convert.ITypeConverter;
import com.github.labai.utils.hardreflect.LaHardCopy.AbstractPojoCopier;

import java.util.function.Function;

/**
 * @author Augustus
 * created on 2023.03.05
 */
public class Copier2 extends AbstractPojoCopier<Pojo1, Pojo2> {
    private ITypeConverter theConv = value -> value;

    private ITypeConverter<Object, Object>[] convArr;

    public Copier2() {
        this.convArr = new ITypeConverter[3];
        this.convArr[0] = theConv;
        this.convArr[1] = theConv;
        this.convArr[2] = theConv;

    }


    @Override
    public Pojo2 copyPojo(Pojo1 from) {
        Pojo2 target = new Pojo2(
            (String) convArr[0].convert(from.getA01()),
            (String) convArr[1].convert(from.getA02()),
            1111111111);
        target.setA03(
            (String) convArr[2].convert(from.getA03())
        );
        target.setA04(
            (String) convArr[3].convert(from.getA04())
        );
        return target;
    }

    @Override
    public void setTypeConverters(ITypeConverter[] typeConverters) {
        this.convArr = typeConverters;
    }

    @Override
    public void setDataSuppliers(Function<Pojo1, ?>[] dataSuppliers) {

    }
}

// // class version 52.0 (52)
//// access flags 0x21
//// signature Ljava/lang/Object;Lcom/github/labai/utils/hardreflect/LaHardCopy$PojoCopier<Lpackage1/Pojo1;Lpackage1/Pojo2;>;
//// declaration: package1/Copier2 implements com.github.labai.utils.hardreflect.LaHardCopy$PojoCopier<package1.Pojo1, package1.Pojo2>
//public class package1/Copier2 implements com/github/labai/utils/hardreflect/LaHardCopy$PojoCopier {
//
//  // compiled from: Copier2.java
//  // access flags 0x609
//  public static abstract INNERCLASS com/github/labai/utils/hardreflect/LaHardCopy$PojoCopier com/github/labai/utils/hardreflect/LaHardCopy PojoCopier
//  // access flags 0x19
//  public final static INNERCLASS java/lang/invoke/MethodHandles$Lookup java/lang/invoke/MethodHandles Lookup
//
//  // access flags 0x2
//  private Lcom/github/labai/utils/convert/ITypeConverter; theConv
//
//  // access flags 0x2
//  // signature [Lcom/github/labai/utils/convert/ITypeConverter<Ljava/lang/Object;Ljava/lang/Object;>;
//  // declaration: convArr extends com.github.labai.utils.convert.ITypeConverter<java.lang.Object, java.lang.Object>[]
//  private [Lcom/github/labai/utils/convert/ITypeConverter; convArr
//
//  // access flags 0x1
//  public <init>()V
//   L0
//    LINENUMBER 22 L0
//    ALOAD 0
//    INVOKESPECIAL java/lang/Object.<init> ()V
//   L1
//    LINENUMBER 11 L1
//    ALOAD 0
//    INVOKEDYNAMIC convert()Lcom/github/labai/utils/convert/ITypeConverter; [
//      // handle kind 0x6 : INVOKESTATIC
//      java/lang/invoke/LambdaMetafactory.metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
//      // arguments:
//      (Ljava/lang/Object;)Ljava/lang/Object;,
//      // handle kind 0x6 : INVOKESTATIC
//      package1/Copier2.lambda$new$0(Ljava/lang/Object;)Ljava/lang/Object;,
//      (Ljava/lang/Object;)Ljava/lang/Object;
//    ]
//    PUTFIELD package1/Copier2.theConv : Lcom/github/labai/utils/convert/ITypeConverter;
//   L2
//    LINENUMBER 23 L2
//    ALOAD 0
//    ICONST_3
//    ANEWARRAY com/github/labai/utils/convert/ITypeConverter
//    PUTFIELD package1/Copier2.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//   L3
//    LINENUMBER 24 L3
//    ALOAD 0
//    GETFIELD package1/Copier2.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_0
//    ALOAD 0
//    GETFIELD package1/Copier2.theConv : Lcom/github/labai/utils/convert/ITypeConverter;
//    AASTORE
//   L4
//    LINENUMBER 25 L4
//    ALOAD 0
//    GETFIELD package1/Copier2.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_1
//    ALOAD 0
//    GETFIELD package1/Copier2.theConv : Lcom/github/labai/utils/convert/ITypeConverter;
//    AASTORE
//   L5
//    LINENUMBER 26 L5
//    ALOAD 0
//    GETFIELD package1/Copier2.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_2
//    ALOAD 0
//    GETFIELD package1/Copier2.theConv : Lcom/github/labai/utils/convert/ITypeConverter;
//    AASTORE
//   L6
//    LINENUMBER 28 L6
//    RETURN
//   L7
//    LOCALVARIABLE this Lpackage1/Copier2; L0 L7 0
//    MAXSTACK = 3
//    MAXLOCALS = 1
//
//  // access flags 0x1
//  public copyPojo(Lpackage1/Pojo1;)Lpackage1/Pojo2;
//   L0
//    LINENUMBER 33 L0
//    NEW package1/Pojo2
//    DUP
//    ALOAD 0
//    GETFIELD package1/Copier2.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_0
//    AALOAD
//    ALOAD 1
//   L1
//    LINENUMBER 34 L1
//    INVOKEVIRTUAL package1/Pojo1.getA01 ()Ljava/lang/String;
//    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String
//    ALOAD 0
//    GETFIELD package1/Copier2.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_1
//    AALOAD
//    ALOAD 1
//   L2
//    LINENUMBER 35 L2
//    INVOKEVIRTUAL package1/Pojo1.getA02 ()Ljava/lang/String;
//    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String
//    LDC 1111111111
//    INVOKESPECIAL package1/Pojo2.<init> (Ljava/lang/String;Ljava/lang/String;I)V
//    ASTORE 2
//   L3
//    LINENUMBER 37 L3
//    ALOAD 2
//    ALOAD 0
//    GETFIELD package1/Copier2.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_2
//    AALOAD
//    ALOAD 1
//   L4
//    LINENUMBER 38 L4
//    INVOKEVIRTUAL package1/Pojo1.getA03 ()Ljava/lang/String;
//    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String
//   L5
//    LINENUMBER 37 L5
//    INVOKEVIRTUAL package1/Pojo2.setA03 (Ljava/lang/String;)V
//   L6
//    LINENUMBER 40 L6
//    ALOAD 2
//    ALOAD 0
//    GETFIELD package1/Copier2.convArr : [Lcom/github/labai/utils/convert/ITypeConverter;
//    ICONST_3
//    AALOAD
//    ALOAD 1
//   L7
//    LINENUMBER 41 L7
//    INVOKEVIRTUAL package1/Pojo1.getA04 ()Ljava/lang/String;
//    INVOKEINTERFACE com/github/labai/utils/convert/ITypeConverter.convert (Ljava/lang/Object;)Ljava/lang/Object; (itf)
//    CHECKCAST java/lang/String
//   L8
//    LINENUMBER 40 L8
//    INVOKEVIRTUAL package1/Pojo2.setA04 (Ljava/lang/String;)V
//   L9
//    LINENUMBER 43 L9
//    ALOAD 2
//    ARETURN
//   L10
//    LOCALVARIABLE this Lpackage1/Copier2; L0 L10 0
//    LOCALVARIABLE from Lpackage1/Pojo1; L0 L10 1
//    LOCALVARIABLE target Lpackage1/Pojo2; L3 L10 2
//    MAXSTACK = 5
//    MAXLOCALS = 3
//
//  // access flags 0x1041
//  public synthetic bridge copyPojo(Ljava/lang/Object;)Ljava/lang/Object;
//   L0
//    LINENUMBER 10 L0
//    ALOAD 0
//    ALOAD 1
//    CHECKCAST package1/Pojo1
//    INVOKEVIRTUAL package1/Copier2.copyPojo (Lpackage1/Pojo1;)Lpackage1/Pojo2;
//    ARETURN
//   L1
//    LOCALVARIABLE this Lpackage1/Copier2; L0 L1 0
//    MAXSTACK = 2
//    MAXLOCALS = 2
//
//  // access flags 0x100A
//  private static synthetic lambda$new$0(Ljava/lang/Object;)Ljava/lang/Object;
//   L0
//    LINENUMBER 11 L0
//    ALOAD 0
//    ARETURN
//   L1
//    LOCALVARIABLE value Ljava/lang/Object; L0 L1 0
//    MAXSTACK = 1
//    MAXLOCALS = 1
//}
