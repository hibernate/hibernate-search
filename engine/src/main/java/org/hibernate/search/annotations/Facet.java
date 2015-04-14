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
 * Extension annotation for {@code @Field} enabling it for faceting.
 *
 * @author Hardy Ferentschik
 * @hsearch.experimental : This feature is experimental
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Documented
public @interface Facet {

	/**
	 * @return the facet name. Defaults to the name of the field this facet refers to
	 */
	String name() default "";

	/**
	 * @return the field name this annotation refers to. It can be omitted in case there is only a single {@code @Field}
	 * annotation
	 */
	String forField() default "";

	/**
	 * @return the encoding type to use for this facet. Per default the encoding type is chosen based on the type of the
	 * entity property.
	 */
	FacetEncodingType encoding() default FacetEncodingType.AUTO;
}
