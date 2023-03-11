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
package com.github.labai.utils.hardreflect;

import com.github.labai.utils.convert.ITypeConverter;
import com.github.labai.utils.hardreflect.LaHardReflect.NameOrAccessor;
import com.github.labai.utils.hardreflect.impl.LaHardCopyImpl;

import java.util.List;
import java.util.function.Function;

/**
 * @author Augustus
 * created on 2023.02.27
 *
 *
 * for internal usage only!
 */
public class LaHardCopy {

    public interface PojoCopier<Fr, To> {
        To copyPojo(Fr from);
        void setTypeConverters(ITypeConverter[] typeConverters);
        void setDataSuppliers(Function<Fr, ?>[] dataSuppliers);
    }

    public abstract static class AbstractPojoCopier<Fr, To> implements PojoCopier<Fr, To> {
        protected ITypeConverter<Object, Object>[] convArr;
        protected Function<Fr, ?>[] dataSupplierArr;

        @Override
        public void setTypeConverters(ITypeConverter[] typeConverters) {
            this.convArr = typeConverters;
        }

        @Override
        public void setDataSuppliers(Function<Fr, ?>[] dataSuppliers) {
            this.dataSupplierArr = dataSuppliers;
        }
    }

    public static class PojoCopyPropDef {
        public final NameOrAccessor source;
        public final Function<?, ?> dataSupplier;
        public final NameOrAccessor target;
        public final ITypeConverter convFn;

        private PojoCopyPropDef(NameOrAccessor source, NameOrAccessor target, ITypeConverter convFn, Function<?, ?> dataSupplier) {
            this.source = source;
            this.dataSupplier = dataSupplier;
            this.target = target;
            this.convFn = convFn;
        }

        public static PojoCopyPropDef forAuto(NameOrAccessor source, NameOrAccessor target, ITypeConverter convFn) {
            return new PojoCopyPropDef(source, target, convFn, null);
        }

        public static <Fr> PojoCopyPropDef forManual(Function<Fr, ?> dataSupplier, NameOrAccessor target, ITypeConverter convFn) {
            return new PojoCopyPropDef(null, target, convFn, dataSupplier);
        }
    }

    public static class PojoArgDef {
        public final NameOrAccessor source;
        public final ITypeConverter convFn;
        public final Class<?> argType;
        public final Object constant;
        public final Function<?, ?> dataSupplier;

        private PojoArgDef(Class<?> argType, NameOrAccessor source, ITypeConverter convFn, Object constant, Function<?, ?> dataSupplier) {
            this.argType = argType;
            this.source = source;
            this.convFn = convFn;
            this.constant = constant;
            this.dataSupplier = dataSupplier;
        }

        public static PojoArgDef forConstant(Class<?> argType, Object constant) {
            return new PojoArgDef(argType, null, null, constant, null);
        }

        public static PojoArgDef forProp(Class<?> argType, NameOrAccessor source, ITypeConverter convFn) {
            return new PojoArgDef(argType, source, convFn, null, null);
        }

        public static PojoArgDef forProp(Class<?> argType, NameOrAccessor source) {
            return new PojoArgDef(argType, source, null, null, null);
        }

        public static PojoArgDef forSupplier(Class<?> argType, Function<?, ?> dataSupplier, ITypeConverter convFn) {
            return new PojoArgDef(argType, null, convFn, null, dataSupplier);
        }
    }

    public static <Fr, To> PojoCopier createPojoCopierClass(
        Class<Fr> pojoFrClass,
        Class<To> pojoToClass,
        List<PojoArgDef> argDefs,
        List<PojoCopyPropDef> propCopyDefs
    ) {
        return LaHardCopyImpl.createPojoCopierClass(pojoFrClass, pojoToClass, argDefs, propCopyDefs);
    }
}
