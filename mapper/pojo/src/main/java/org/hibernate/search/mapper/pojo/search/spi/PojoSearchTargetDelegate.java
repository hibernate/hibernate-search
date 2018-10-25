/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.search.spi;

import java.util.Set;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;

public interface PojoSearchTargetDelegate<T> {

	Set<Class<? extends T>> getTargetedIndexedTypes();

	<O> SearchQueryResultDefinitionContext<PojoReference, O> query(ObjectLoader<PojoReference, O> objectLoader);

	SearchPredicateFactoryContext predicate();

	SearchSortContainerContext sort();

	SearchProjectionFactoryContext projection();

}
