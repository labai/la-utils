package com.github.labai.deci.converter.jpa2;

/**
 * @author Augustus
 * created on 2020.11.27
 *
 * can be used to for package scanning configuration
 *
 * factory.setPackagesToScan(PACKAGES_DOMAIN, JpaConverters.PACKAGE);
 *
 */
public final class Jpa2DeciRegister {
    public static final String PACKAGE = Jpa2DeciRegister.class.getPackage().getName();
}
