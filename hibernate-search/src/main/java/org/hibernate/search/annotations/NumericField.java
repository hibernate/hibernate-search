package org.hibernate.search.annotations;

import org.apache.lucene.util.NumericUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Extension annotation for  <code>@Field</code>
 *
 * author: Gustavo Fernandes
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.METHOD, ElementType.FIELD } )
public @interface NumericField {
	/**
	 * Precision step for numeric field. The less, more terms will be present in the index
	 */
	int precisionStep() default NumericUtils.PRECISION_STEP_DEFAULT;

	/**
	 * Field name it refers to
	 */
	String forField() default "";
}
