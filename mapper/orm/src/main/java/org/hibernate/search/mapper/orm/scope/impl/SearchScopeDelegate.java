/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.scope.impl;

import java.util.Set;

import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.aggregation.dsl.spi.SearchAggregationFactoryDelegate;
import org.hibernate.search.engine.search.common.NonStaticMetamodelScope;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.spi.SearchPredicateFactoryDelegate;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.spi.SearchProjectionFactoryDelegate;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.spi.SearchSortFactoryDelegate;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmSelectionLoadingContext;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.orm.spi.BatchScopeContext;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;

@SuppressWarnings({ "deprecation", "removal" })
public record SearchScopeDelegate<E>(TypedSearchScopeImpl<NonStaticMetamodelScope, E> delegate)
		implements SearchScope<E>,
		BatchScopeContext<E>,
		SearchScopeSearcher<NonStaticMetamodelScope, E> {

	@Override
	public SearchPredicateFactory predicate() {
		return new SearchPredicateFactoryDelegate( delegate.predicate() );
	}

	@Override
	public SearchProjectionFactory<EntityReference, E> projection() {
		return new SearchProjectionFactoryDelegate<>( delegate.projection() );
	}

	@Override
	public SearchAggregationFactory aggregation() {
		return new SearchAggregationFactoryDelegate( delegate.aggregation() );
	}

	@Override
	public SearchSortFactory sort() {
		return new SearchSortFactoryDelegate( delegate.sort() );
	}

	@Override
	public SearchSchemaManager schemaManager() {
		return delegate.schemaManager();
	}

	@Override
	public SearchHighlighterFactory highlighter() {
		return delegate.highlighter();
	}

	@Override
	public SearchWorkspace workspace() {
		return delegate.workspace();
	}

	@Override
	public SearchWorkspace workspace(String tenantId) {
		return delegate.workspace( tenantId );
	}

	@Override
	public SearchWorkspace workspace(Object tenantId) {
		return delegate.workspace( tenantId );
	}

	@Override
	public MassIndexer massIndexer() {
		return delegate.massIndexer();
	}

	@Override
	public MassIndexer massIndexer(String tenantId) {
		return delegate.massIndexer( tenantId );
	}

	@Override
	public MassIndexer massIndexer(Object tenantId) {
		return delegate.massIndexer( tenantId );
	}

	@Override
	public MassIndexer massIndexer(Set<?> tenantIds) {
		return delegate.massIndexer( tenantIds );
	}

	@Override
	public Set<? extends SearchIndexedEntity<? extends E>> includedTypes() {
		return delegate.includedTypes();
	}

	@Override
	public <T> T extension(IndexScopeExtension<T> extension) {
		return delegate.extension( extension );
	}

	@Override
	public PojoScopeWorkspace pojoWorkspace(String tenantId) {
		return delegate.pojoWorkspace( tenantId );
	}

	@Override
	public SearchQuerySelectStep<NonStaticMetamodelScope, ?, EntityReference, E, SearchLoadingOptionsStep, ?, ?> search(
			HibernateOrmScopeSessionContext sessionContext, HibernateOrmSelectionLoadingContext.Builder loadingContextBuilder) {
		return delegate.search( sessionContext, loadingContextBuilder );
	}
}
