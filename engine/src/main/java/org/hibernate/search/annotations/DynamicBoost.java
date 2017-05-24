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

import org.hibernate.search.engine.BoostStrategy;

/**
 * Apply a dynamic boost factor on a field or a whole entity.
 *
 * @author Hardy Ferentschik
 *
 * @deprecated Index-time boosting will not be possible anymore starting from Lucene 7.
 * You should use query-time boosting instead, for instance by calling
 * {@link org.hibernate.search.query.dsl.FieldCustomization#boostedTo(float) boostedTo(float)}
 * when building queries with the Hibernate Search query DSL.
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Documented
public @interface DynamicBoost {

	/**
	 * @return An implementation of <code>BoostStrategy</code> to apply a boost
	 *         value as function of the annotated object.
	 * @see org.hibernate.search.engine.BoostStrategy
	 */
	Class<? extends BoostStrategy> impl();

}
