/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.scope.impl;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.common.EntityReference;
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
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoLoadingContext;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.massindexing.impl.StandalonePojoMassIndexer;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.pojo.standalone.schema.management.impl.SearchSchemaManagerImpl;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScope;
import org.hibernate.search.mapper.pojo.standalone.tenancy.impl.TenancyConfiguration;
import org.hibernate.search.mapper.pojo.standalone.work.SearchWorkspace;
import org.hibernate.search.mapper.pojo.standalone.work.impl.SearchWorkspaceImpl;
import org.hibernate.search.util.common.impl.Contracts;

public class SearchScopeImpl<E> implements SearchScope<E> {

	private final StandalonePojoScopeMappingContext mappingContext;
	private final PojoScopeDelegate<EntityReference, E, SearchIndexedEntity<? extends E>> delegate;
	private final TenancyConfiguration tenancyConfiguration;

	public SearchScopeImpl(StandalonePojoScopeMappingContext mappingContext,
			TenancyConfiguration tenancyConfiguration,
			PojoScopeDelegate<EntityReference, E, SearchIndexedEntity<? extends E>> delegate) {
		this.mappingContext = mappingContext;
		this.tenancyConfiguration = tenancyConfiguration;
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
		return workspace( (Object) null );
	}

	@Override
	@SuppressWarnings("removal")
	public SearchWorkspace workspace(String tenantId) {
		return workspace( tenantId );
	}

	@Override
	public SearchWorkspace workspace(Object tenantId) {
		return new SearchWorkspaceImpl( delegate.workspace( tenancyConfiguration.convert( tenantId ) ) );
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
			PojoSelectionLoadingContextBuilder<?> loadingContextBuilder) {
		return delegate.search( sessionContext, loadingContextBuilder );
	}

	@Override
	public MassIndexer massIndexer() {
		return massIndexer( Collections.emptySet() );
	}

	@Override
	@SuppressWarnings("removal")
	public MassIndexer massIndexer(String tenantId) {
		Contracts.assertNotNull( tenantId, "tenant identifier" );
		return massIndexer( Collections.singleton( tenantId ) );
	}

	@Override
	public MassIndexer massIndexer(Object tenantId) {
		return null;
	}

	@Override
	public MassIndexer massIndexer(Set<?> tenantIds) {
		StandalonePojoLoadingContext context = mappingContext.loadingContextBuilder().build();
		PojoMassIndexer massIndexerDelegate = delegate.massIndexer( context,
				tenantIds.stream().map( tenancyConfiguration::convert ).collect( Collectors.toUnmodifiableSet() ) );
		return new StandalonePojoMassIndexer( massIndexerDelegate, context );
	}

	public PojoScopeSchemaManager schemaManagerDelegate() {
		return delegate.schemaManager();
	}
}
