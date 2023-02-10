/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.scope.impl;

import java.util.Collections;
import java.util.Set;

import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexer;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeSessionContext;
import org.hibernate.search.mapper.pojo.standalone.common.EntityReference;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoLoadingContext;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.massindexing.impl.StandalonePojoMassIndexer;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.pojo.standalone.schema.management.impl.SearchSchemaManagerImpl;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScope;
import org.hibernate.search.mapper.pojo.standalone.work.SearchWorkspace;
import org.hibernate.search.mapper.pojo.standalone.work.impl.SearchWorkspaceImpl;
import org.hibernate.search.util.common.impl.Contracts;

public class SearchScopeImpl<E> implements SearchScope<E> {

	private final StandalonePojoScopeMappingContext mappingContext;
	private final PojoScopeDelegate<EntityReference, E, StandalonePojoScopeIndexedTypeContext<? extends E>> delegate;

	public SearchScopeImpl(StandalonePojoScopeMappingContext mappingContext,
			PojoScopeDelegate<EntityReference, E, StandalonePojoScopeIndexedTypeContext<? extends E>> delegate) {
		this.mappingContext = mappingContext;
		this.delegate = delegate;
	}

	@Override
	public SearchPredicateFactory predicate() {
		return delegate.predicate();
	}

	@Override
	public SearchSortFactory sort() {
		return delegate.sort();
	}

	@Override
	public SearchProjectionFactory<EntityReference, ?> projection() {
		return delegate.projection();
	}

	@Override
	public SearchAggregationFactory aggregation() {
		return delegate.aggregation();
	}

	@Override
	public SearchHighlighterFactory highlighter() {
		return delegate.highlighter();
	}

	@Override
	public SearchSchemaManager schemaManager() {
		return new SearchSchemaManagerImpl( schemaManagerDelegate() );
	}

	@Override
	public SearchWorkspace workspace() {
		return workspace( (String) null );
	}

	@Override
	public SearchWorkspace workspace(String tenantId) {
		return new SearchWorkspaceImpl( delegate.workspace( tenantId ) );
	}

	@Override
	public Set<? extends SearchIndexedEntity<? extends E>> includedTypes() {
		return delegate.includedIndexedTypes();
	}

	@Override
	public <T> T extension(IndexScopeExtension<T> extension) {
		return delegate.extension( extension );
	}

	public SearchQuerySelectStep<?, EntityReference, E, ?, ?, ?> search(PojoScopeSessionContext sessionContext,
			DocumentReferenceConverter<EntityReference> documentReferenceConverter,
			PojoSelectionLoadingContextBuilder<?> loadingContextBuilder) {
		return delegate.search( sessionContext, documentReferenceConverter, loadingContextBuilder );
	}

	@Override
	public MassIndexer massIndexer() {
		return massIndexer( Collections.emptySet() );
	}

	@Override
	public MassIndexer massIndexer(String tenantId) {
		Contracts.assertNotNull( tenantId, "tenant identifier" );
		return massIndexer( Collections.singleton( tenantId ) );
	}

	@Override
	public MassIndexer massIndexer(Set<String> tenantIds) {
		StandalonePojoLoadingContext context = mappingContext.loadingContextBuilder().build();
		PojoMassIndexer massIndexerDelegate = delegate.massIndexer( context, tenantIds );
		return new StandalonePojoMassIndexer( massIndexerDelegate, context );
	}

	public PojoScopeSchemaManager schemaManagerDelegate() {
		return delegate.schemaManager();
	}
}
