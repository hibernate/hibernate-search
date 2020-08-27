/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.scope.spi;

import java.util.Set;

import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;

public interface V5MigrationSearchScope {

	Set<Class<?>> targetTypes();

	Set<IndexManager> indexManagers();

	SearchPredicateFactory predicate();

	SearchSortFactory sort();

	SearchProjectionFactory<?, ?> projection();

	SearchProjection<Object> idProjection();

	SearchProjection<? extends Class<?>> objectClassProjection();

	SearchAggregationFactory aggregation();

}
