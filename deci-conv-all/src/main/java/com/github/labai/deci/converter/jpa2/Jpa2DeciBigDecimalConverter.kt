package com.github.labai.deci.converter.jpa2

import com.github.labai.deci.Deci
import com.github.labai.deci.deci
import java.math.BigDecimal
import javax.persistence.AttributeConverter
import javax.persistence.Converter

/**
 * @author Augustus
 *         created on 2020.11.27
 */
@Converter(autoApply = true)
class Jpa2DeciBigDecimalConverter : AttributeConverter<Deci?, BigDecimal?> {

    override fun convertToDatabaseColumn(attribute: Deci?): BigDecimal? {
        return attribute?.toBigDecimal()
    }

    override fun convertToEntityAttribute(dbData: BigDecimal?): Deci? {
        return dbData?.deci
    }
}
