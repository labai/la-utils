package com.github.labai.utils.convert;

import com.github.labai.utils.convert.LaConvUtils.ClassPairMap;
import com.github.labai.utils.convert.ext.DeciConverters;

/**
 * @author Augustus
 * created on 2022.11.19
 *
 * usage
 * LaConverterRegistry registry = new LaConverterRegistry();
 * converter = registry.getConverter(String.class, Long.class);
 * Long res = converter.convert("12");
 */
public class LaConverterRegistry implements IConverterResolver {

    // global instance, can be used statically
    public final static IConverterResolver global = new LaConverterRegistry();

    static {
        // additional known types:
        // Deci
        DeciConverters.registryDeciConverters((LaConverterRegistry) global);
    }

    private final ClassPairMap<ITypeConverter<?, ?>> extConverters = new ClassPairMap<>();

    public <Fr, To> void registerConverter(Class<Fr> sourceType, Class<To> targetType, ITypeConverter<Fr, To> converter) {
        extConverters.getOrPut(sourceType, targetType, () -> converter);
    }

    public static <Fr, To> void registerGlobalConverter(Class<Fr> sourceType, Class<To> targetType, ITypeConverter<Fr, To> converter) {
        ((LaConverterRegistry) global).extConverters.getOrPut(sourceType, targetType, () -> converter);
    }

    @SuppressWarnings({"unchecked"})
    public <Fr, To> ITypeConverter<Fr, To> getConverter(Class<Fr> sourceType, Class<To> targetType) {
        // equal match
        if (sourceType.equals(targetType)) {
            return it -> (To) it;
        }

        // external converters
        ITypeConverter<?, ?> extConv = extConverters.get(sourceType, targetType);
        if (extConv != null)
            return (ITypeConverter<Fr, To>) extConv;

        // external global converters
        extConv = ((LaConverterRegistry) global).extConverters.get(sourceType, targetType);
        if (extConv != null)
            return (ITypeConverter<Fr, To>) extConv;

        // any type can be converted to String
        if (targetType == String.class) {
            return value -> (To) String.valueOf(value);
        }

        // common converters
        ITypeConverter<Fr, To> convFn = StdConverters.chooseStdConverter(sourceType, targetType);
        if (convFn != null)
            return convFn;

        // parent-subclass match
        if (targetType.isAssignableFrom(sourceType)) {
            return targetType::cast;
        }

        throw new LaConvertException("Convert case is not defined (field.class=" + targetType.getCanonicalName() + ", value.class=" + sourceType.getCanonicalName() + ")");
    }
}
