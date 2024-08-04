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
package com.github.labai.utils.mapper;

import kotlin.Pair;

import java.util.List;
import java.util.function.Function;

/**
 * @author Augustus
 * created on 2023.05.10
 *
 * Simplified for Java usage.
 *
 * Uses 'global' instance of LaMapper, so default configuration in LaMapper is valid here.
 * Also, it uses 'global' laConverter instance, so converters can be setup directly in LaConverterRegistry:
 *  - LaConverterRegistry.registerGlobalConverter()
 *  - LaConverterRegistry.registerGlobalExtResolver()
 *
 */
public final class LaMapperJ {

    private LaMapperJ() {
    }

    @SuppressWarnings("unchecked")
    public static <Fr, To> To copyFrom(Fr fr, Class<To> targetClass) {
        if (fr == null)
            return null;
        return LaMapper.Companion.getGlobal().copyFromJ(fr, (Class<Fr>) fr.getClass(), targetClass, null);
    }

    @SuppressWarnings("unchecked")
    public static <Fr, To> To copyFrom(Fr fr, Class<To> targetClass, List<Pair<String, Function<Fr, ?>>> fieldMappers) {
        if (fr == null)
            return null;
        return LaMapper.Companion.getGlobal().copyFromJ(fr, (Class<Fr>) fr.getClass(), targetClass, fieldMappers);
    }

    public static <Fr, To> AutoMapper<Fr, To> autoMapper(Class<Fr> sourceClass, Class<To> targetClass) {
        return LaMapper.Companion.getGlobal().autoMapperJ(sourceClass, targetClass, null);
    }

    public static <Fr, To> AutoMapper<Fr, To> autoMapper(Class<Fr> sourceClass, Class<To> targetClass, List<Pair<String, Function<Fr, ?>>> fieldMappers) {
        return LaMapper.Companion.getGlobal().autoMapperJ(sourceClass, targetClass, fieldMappers);
    }

    public static <Fr> Pair<String, Function<Fr, ?>> mapFrom(String t, Function<Fr, ?> f) {
        return new Pair<>(t, f);
    }
}
