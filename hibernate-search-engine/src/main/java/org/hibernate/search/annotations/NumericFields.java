package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Numeric extension for Fields annotation
 *
 * @author Gustavo Fernandes
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.METHOD, ElementType.FIELD} )
@Documented
public @interface NumericFields {

	/**
	 * NumericFields
	 */
	NumericField[] value();
}
