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
import java.lang.annotation.Documented;

/**
 * Defines a FullTextFilter that can be optionally applied to
 * every FullText Queries
 * While not related to a specific indexed entity, the annotation has to be set on one of them
 *
 * @author Emmanuel Bernard
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( { ElementType.TYPE } )
@Documented
public @interface FullTextFilterDef {
	/**
	 * @return the filter name. Must be unique across all mappings for a given persistence unit
	 */
	String name();

	/**
	 * Either implements {@link org.apache.lucene.search.Filter}
	 * or contains a <code>@Factory</code> method returning one.
	 * The generated <code>Filter</code> must be thread-safe.
	 *
	 * If the filter accept parameters, an <code>@Key</code> method must be present as well.
	 *
	 * @return a class which either implements <code>Filter</code> directly or contains a method annotated with
	 * <code>@Factory</code>.
	 *
	 */
	Class<?> impl();

	/**
	 * @return The cache mode for the filter. Default to instance and results caching
	 */
	FilterCacheModeType cache() default FilterCacheModeType.INSTANCE_AND_DOCIDSETRESULTS;
}
