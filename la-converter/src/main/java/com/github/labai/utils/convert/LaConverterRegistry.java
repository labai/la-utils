package com.github.labai.utils.convert;

import com.github.labai.utils.convert.LaConvUtils.ClassPairMap;
import com.github.labai.utils.convert.ext.DeciConverters;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Augustus
 * created on 2022.11.19
 *
 * usage
 * LaConverterRegistry registry = new LaConverterRegistry();
 * converter = registry.getConverter(String.class, Long.class);
 * Long res = converter.convert("12");
 *
 * return null if not found
 */
public class LaConverterRegistry implements IConverterResolver {

    // global instance, can be used statically
    public final static IConverterResolver global = new LaConverterRegistry();

    static {
        // additional known types:
        // Deci
        DeciConverters.registryDeciConverters((LaConverterRegistry) global);
    }

    private final Set<IConverterResolver> extResolvers = new LinkedHashSet<>();
    private final ClassPairMap<ITypeConverter<?, ?>> extConverters = new ClassPairMap<>();

    public <Fr, To> void registerConverter(Class<Fr> sourceType, Class<To> targetType, ITypeConverter<Fr, To> converter) {
        extConverters.getOrPut(sourceType, targetType, () -> converter);
    }

    public <Fr, To> void registerExtResolver(IConverterResolver resolver) {
        extResolvers.add(resolver);
    }

    public static <Fr, To> void registerGlobalConverter(Class<Fr> sourceType, Class<To> targetType, ITypeConverter<Fr, To> converter) {
        ((LaConverterRegistry) global).extConverters.getOrPut(sourceType, targetType, () -> converter);
    }

    public static <Fr, To> void registerGlobalExtResolver(IConverterResolver resolver) {
        ((LaConverterRegistry) global).extResolvers.add(resolver);
    }

    @SuppressWarnings({"unchecked"})
    public <Fr, To> ITypeConverter<Fr, To> getConverter(Class<Fr> sourceType, Class<To> targetType) {
        // equal match
        if (sourceType.equals(targetType)) {
            return it -> (To) it;
        }

        // external resolver
        for (IConverterResolver resolver : extResolvers) {
            ITypeConverter<Fr, To> extConv = resolver.getConverter(sourceType, targetType);
            if (extConv != null)
                return extConv;
        }

        // external converters
        {
            ITypeConverter<?, ?> extConv = extConverters.get(sourceType, targetType);
            if (extConv != null)
                return (ITypeConverter<Fr, To>) extConv;
        }

        // also check global config (if this is not global)
        if (global != this) {
            // external global resolver
            for (IConverterResolver resolver : ((LaConverterRegistry) global).extResolvers) {
                ITypeConverter<Fr, To> extConv = resolver.getConverter(sourceType, targetType);
                if (extConv != null)
                    return extConv;
            }
            // external global converters
            {
                ITypeConverter<?, ?> extConv = ((LaConverterRegistry) global).extConverters.get(sourceType, targetType);
                if (extConv != null)
                    return (ITypeConverter<Fr, To>) extConv;
            }
        }

        // any type can be converted to String
        if (targetType == String.class) {
            return value -> (To) (value == null ? null : String.valueOf(value));
        }

        // common converters
        ITypeConverter<Fr, To> convFn = StdConverters.chooseStdConverter(sourceType, targetType);
        if (convFn != null)
            return convFn;

        // parent-subclass match
        if (targetType.isAssignableFrom(sourceType)) {
            return targetType::cast;
        }

        return null;
    }
}
