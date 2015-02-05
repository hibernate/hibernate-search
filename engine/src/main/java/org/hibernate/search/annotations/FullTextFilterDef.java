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
 * Defines a FullTextFilter that can be optionally applied to
 * every FullText Queries
 * While not related to a specific indexed entity, the annotation has to be set on one of them
 *
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.PACKAGE, ElementType.TYPE } )
@Documented
public @interface FullTextFilterDef {

	/**
	 * @return the filter name. Must be unique across all mappings for a given persistence unit
	 */
	String name();

	/**
	 * The implementation of this filter definition. May be
	 * <ul>
	 * <li>a class implementing {@link org.apache.lucene.search.Filter} or</li>
	 * <li>a filter factory class, defining a method annotated with {@link Factory} which has no parameters and returns
	 * a {@code Filter} instance.</li>
	 * </ul>
	 * The given class must define a no-args constructor and a JavaBeans setter method for each parameter to be passed
	 * via {@link org.hibernate.search.filter.FullTextFilter#setParameter(String, Object)}.
	 * <p>
	 * The Lucene filter created by this filter definition must be thread-safe.
	 *
	 * @return A class implementing {@code Filter} or a filter factory class
	 */
	Class<?> impl();

	/**
	 * @return The cache mode for the filter. Default to instance and results caching
	 */
	FilterCacheModeType cache() default FilterCacheModeType.INSTANCE_AND_DOCIDSETRESULTS;
}
