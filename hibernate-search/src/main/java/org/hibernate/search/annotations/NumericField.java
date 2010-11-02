package org.hibernate.search.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Extension annotation for  <code>@Field</code>
 *
 * @author: Gustavo Fernandes
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.METHOD, ElementType.FIELD } )
public @interface NumericField {
	/**
	 * Precision step for numeric field. The less, more terms will be present in the index
	 */
	int precisionStep() default PRECISION_STEP_DEFAULT;

	/**
	 * Field name it refers to. If omitted, refers to the @Field annotation in case there's only one
	 */
	String forField() default "";

	/**
	 * Default precision step.
	 * (Mimic Lucene's default precision step value)
	 */
	static final int PRECISION_STEP_DEFAULT = 4;
}
