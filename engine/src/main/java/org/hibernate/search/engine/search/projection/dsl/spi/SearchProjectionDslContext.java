/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.projection.dsl.spi;

import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;

/**
 * Represents the current context in the search DSL,
 * including in particular the projection builder factory.
 *
 * @param <F> The type of predicate builder factory.
 */
public interface SearchProjectionDslContext<F extends SearchProjectionBuilderFactory> {

	/**
	 * @return The aggregation builder factory. Will always return the exact same instance.
	 */
	F builderFactory();

}
