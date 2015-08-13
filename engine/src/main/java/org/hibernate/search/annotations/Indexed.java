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

import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;

@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Documented
/**
 * Specifies that an entity is to be indexed by Lucene
 */
public @interface Indexed {
	/**
	 * @return The filename of the index. Default to empty string
	 */
	String index() default "";

	/**
	 * Custom converter to change operations upon indexing
	 * Useful for soft deletes and similar patterns
	 *
	 * @return the custom {@link EntityIndexingInterceptor} class. Default to {@link EntityIndexingInterceptor} class
	 * @hsearch.experimental : This feature is experimental
	 */
	Class<? extends EntityIndexingInterceptor> interceptor() default EntityIndexingInterceptor.class;
}
