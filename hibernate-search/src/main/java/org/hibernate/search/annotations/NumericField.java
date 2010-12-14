package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Extension annotation for {@code @Field} supporting Lucene's numeric field feature.
 *
 * @author Gustavo Fernandes
 * @experimental Lucene marks the numeric field API still as experimental and warns for incompatible changes in coming
 * releases. Using Hibernate Search will hopefully shield you from any underlying API changes, but that is not guaranteed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Documented
public @interface NumericField {
	/**
	 * @return Precision step for numeric field. The less, more terms will be present in the index.
	 */
	int precisionStep() default PRECISION_STEP_DEFAULT;

	/**
	 * @return Field name this annotation refers to. If omitted, refers to the @Field annotation in case there's only one
	 */
	String forField() default "";

	/**
	 * Default precision step, mimicking  Lucene's default precision step value.
	 */
	static final int PRECISION_STEP_DEFAULT = 4;
}
