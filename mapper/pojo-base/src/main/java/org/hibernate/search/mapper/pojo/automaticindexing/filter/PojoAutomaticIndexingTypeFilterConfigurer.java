/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.automaticindexing.filter;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Configurer implementers are responsible for specifying which indexed types should be included/excluded from indexing.
 * <p>
 * Usually a lambda is expected by the method that receive this type as an input parameter.
 */
@Incubating
public interface PojoAutomaticIndexingTypeFilterConfigurer {
	/**
	 * This method is invoked by the indexing filter to configure itself.
	 *
	 * @param context The context exposing include/exclude methods to configure the filter.
	 */
	void configure(PojoAutomaticIndexingTypeFilterContext context);

}
