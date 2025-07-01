/*
The MIT License (MIT)

Copyright (c) 2025 Augustus

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
package com.github.labai.utils.targetbuilder;

import com.github.labai.utils.convert.LaConverterRegistry;
import com.github.labai.utils.mapper.LaMapper;
import com.github.labai.utils.mapper.impl.DataConverters;
import com.github.labai.utils.mapper.impl.ServiceContext;
import com.github.labai.utils.targetbuilder.impl.TargetBuilderFactory;
import com.github.labai.utils.targetbuilder.impl.TargetBuilderStringFactory;

/*
 * @author Augustus
 * created on 2025-06-29
 *
 * (experimental)
 *
 *  * Builder to build an object of type <targetKlass> by providing String values:
 *
 * Usage example:
 *
 * val factory = TargetBuilderStringFactory(SamplePojo::class, serviceContext)
 * val pojo: SamplePojo = factory.instance()
 *     .add("name", "Vardas")
 *     .add("age", "18")
 *     .add("address", "Vilnius")
 *     .build()
 *
 * This implementation takes string values, it may be useful for various parsers
 *
 */
public class TargetBuilderJ {
    private static ServiceContext _serviceContext = null;

    private TargetBuilderJ() {
    }

    // pojo builder for class
    public static <To> ITargetBuilderFactory<To> forClass(Class<To> targetClass) {
        var klass = kotlin.jvm.JvmClassMappingKt.getKotlinClass(targetClass);
        return new TargetBuilderFactory<>(klass, getServiceContext());
    }

    // pojo builder from strings (can be used for parsers)
    public static <To> ITargetBuilderStringFactory<To> fromStringsSource(Class<To> targetClass) {
        var klass = kotlin.jvm.JvmClassMappingKt.getKotlinClass(targetClass);
        return new TargetBuilderStringFactory<>(klass, getServiceContext());
    }

    private static ServiceContext getServiceContext() {
        if (_serviceContext != null)
            return _serviceContext;
        synchronized (TargetBuilderJ.class) {
            var ctx = new ServiceContext();
            ctx.config = new LaMapper.LaMapperConfig();
            ctx.dataConverters = new DataConverters(LaConverterRegistry.global, ctx.config);
            _serviceContext = ctx;
        }
        return _serviceContext;
    }
}
