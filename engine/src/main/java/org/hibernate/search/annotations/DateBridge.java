/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the temporal resolution and encoding type of a {@code java.util.Date} field.
 *
 * <p>
 * <b>Note:</b> Dates are encoded in the GMT timezone.
 *
 * @author Emmanuel Bernard
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface DateBridge {
	/**
	 * @return the resolution for the annotated date. The date will be rounded to the specified resolution.
	 */
	Resolution resolution();

	/**
	 * @return the encoding type for the annotated date.
	 */
	EncodingType encoding() default EncodingType.NUMERIC;
}
