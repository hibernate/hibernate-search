/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Extension annotation for {@code @Field} supporting Lucene's numeric field feature.
 *
 * @author Gustavo Fernandes
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Documented
@Repeatable(NumericFields.class)
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
	int PRECISION_STEP_DEFAULT = 4;
}
