/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.scope.spi;

import java.util.Collection;
import java.util.Set;

import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexer;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;

/**
 * @param <R> The type of entity references, i.e. the type of hits returned by
 * {@link SearchQuerySelectStep#selectEntityReference()} reference queries},
 * @param <E> The type of loaded entities, i.e. the type of hits returned by
 * {@link SearchQuerySelectStep#selectEntity() entity queries},
 * or the type of objects returned for {@link SearchProjectionFactory#entity() entity projections}.
 * @param <C> The type of indexed type extended contexts; i.e. the type of elements in the set returned by
 * {@link #includedIndexedTypes()}.
 * or the type of objects returned for {@link SearchProjectionFactory#entity() entity projections}.
 */
public interface PojoScopeDelegate<R, E, C> {

	Set<C> includedIndexedTypes();

	<LOS> SearchQuerySelectStep<?, R, E, LOS, SearchProjectionFactory<R, E>, ?> search(
			PojoScopeSessionContext sessionContext, DocumentReferenceConverter<R> documentReferenceConverter,
			PojoSelectionLoadingContextBuilder<LOS> loadingContextBuilder);

	SearchPredicateFactory predicate();

	SearchSortFactory sort();

	SearchProjectionFactory<R, E> projection();

	SearchAggregationFactory aggregation();

	/**
	 * @param sessionContext The detached session, for the tenant ID.
	 * @return A {@link PojoScopeWorkspace}.
	 * @deprecated Use {@link #workspace(String)} instead.
	 */
	@Deprecated
	PojoScopeWorkspace workspace(org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext sessionContext);

	PojoScopeWorkspace workspace(String tenantId);

	PojoScopeSchemaManager schemaManager();

	/**
	 * @param context The mass indexing context.
	 * @param detachedSession The detached session, for the tenant ID.
	 * @return A {@link PojoMassIndexer}.
	 * @deprecated Use {@link #massIndexer(PojoMassIndexingContext, Collection)} instead.
	 */
	@Deprecated
	PojoMassIndexer massIndexer(PojoMassIndexingContext context, org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext detachedSession);

	PojoMassIndexer massIndexer(PojoMassIndexingContext context, Collection<String> tenantIds);

	<T> T extension(IndexScopeExtension<T> extension);
}
