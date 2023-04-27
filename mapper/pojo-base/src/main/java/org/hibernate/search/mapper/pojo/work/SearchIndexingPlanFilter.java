/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Configurer implementers are responsible for specifying which types should be included/excluded from indexing.
 * <p>
 * Usually a lambda is expected by the method that receive this type as an input parameter.
 */
@Incubating
@FunctionalInterface
public interface SearchIndexingPlanFilter {
	/**
	 * This method is invoked while filtering types to be indexed.
	 *
	 * @param context The context exposing include/exclude methods to configure the filter.
	 */
	void apply(SearchIndexingPlanFilterContext context);

}
