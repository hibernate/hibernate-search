/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Documented;

/**
 * Reusable normalizer definition.
 * <p>
 * A normalizer definition defines one or more filters,
 * but on contrary to analyzer it doesn't include any tokenizer.
 * On top of that, filters should not perform any kind of tokenization.
 * This allows normalizer to be used on fields where we only want a single indexed value,
 * such as sortable fields.
 * <p>
 * Hibernate Search will try to validate that no tokenization is performed at runtime.
 * <p>
 * Filters are applied in the order they are defined.
 *
 * @author Emmanuel Bernard
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PACKAGE, ElementType.TYPE, ElementType.FIELD, ElementType.METHOD })
@Documented
@Repeatable(NormalizerDefs.class)
public @interface NormalizerDef {
	/**
	 * @return Reference name to be used on {@link org.hibernate.search.annotations.Normalizer}
	 */
	String name();

	/**
	 * @return CharFilters used. The filters are applied in the defined order
	 */
	CharFilterDef[] charFilters() default { };

	/**
	 * @return Filters used. The filters are applied in the defined order
	 */
	TokenFilterDef[] filters() default { };
}
