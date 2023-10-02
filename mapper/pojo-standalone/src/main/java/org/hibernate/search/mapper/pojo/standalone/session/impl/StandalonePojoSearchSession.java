/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.session.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.asSetIgnoreNull;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.function.Consumer;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.spi.AbstractPojoSearchSession;
import org.hibernate.search.mapper.pojo.standalone.loading.dsl.SelectionLoadingOptionsStep;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoLoadingContext;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoLoadingSessionContext;
import org.hibernate.search.mapper.pojo.standalone.loading.impl.StandalonePojoSelectionLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.standalone.logging.impl.Log;
import org.hibernate.search.mapper.pojo.standalone.mapping.impl.ConfiguredIndexingPlanSynchronizationStrategyHolder;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.massindexing.impl.StandalonePojoMassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScope;
import org.hibernate.search.mapper.pojo.standalone.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSessionBuilder;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexer;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.standalone.work.SearchWorkspace;
import org.hibernate.search.mapper.pojo.standalone.work.impl.SearchIndexerImpl;
import org.hibernate.search.mapper.pojo.standalone.work.impl.SearchIndexingPlanImpl;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredSearchIndexingPlanFilter;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class StandalonePojoSearchSession extends AbstractPojoSearchSession
		implements SearchSession, StandalonePojoMassIndexingSessionContext, StandalonePojoLoadingSessionContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final ConfiguredSearchIndexingPlanFilter ACCEPT_ALL = typeIdentifier -> true;

	private final StandalonePojoSearchSessionMappingContext mappingContext;
	private final StandalonePojoSearchSessionTypeContextProvider typeContextProvider;

	private final String tenantId;

	private final Consumer<SelectionLoadingOptionsStep> loadingOptionsContributor;
	private final ConfiguredIndexingPlanSynchronizationStrategyHolder synchronizationStrategyHolder;
	private final ConfiguredSearchIndexingPlanFilter configuredIndexingPlanFilter;

	private SearchIndexingPlanImpl indexingPlan;
	private SearchIndexer indexer;
	private boolean open = true;
	private ConfiguredIndexingPlanSynchronizationStrategy indexingPlanSynchronizationStrategy;

	private StandalonePojoSearchSession(Builder builder) {
		super( builder.mappingContext );
		this.mappingContext = builder.mappingContext;
		this.typeContextProvider = builder.typeContextProvider;
		this.tenantId = builder.tenantId;
		this.loadingOptionsContributor = builder.loadingOptionsContributor;
		this.synchronizationStrategyHolder = builder.synchronizationStrategyHolder;

		this.indexingPlanSynchronizationStrategy =
				this.synchronizationStrategyHolder.configureOverriddenSynchronizationStrategy(
						builder.synchronizationStrategy );
		this.configuredIndexingPlanFilter = ACCEPT_ALL;
	}

	private void checkOpenAndThrow() {
		if ( !open ) {
			throw log.hibernateSessionAccessError( "is closed" );
		}
	}

	@Override
	public void close() {
		if ( !open ) {
			return;
		}
		open = false;
		if ( indexingPlan != null ) {
			indexingPlan.execute();
		}
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public MassIndexer massIndexer(Collection<? extends Class<?>> classes) {
		checkOpenAndThrow();
		return scope( classes ).massIndexer( asSetIgnoreNull( this.tenantIdentifierValue() ) );
	}

	@Override
	@SuppressWarnings("removal")
	public String tenantIdentifier() {
		return tenantId;
	}

	@Override
	public Object tenantIdentifierValue() {
		return mappingContext.tenancyConfiguration().convert( tenantId );
	}

	@Override
	public void indexingPlanSynchronizationStrategy(IndexingPlanSynchronizationStrategy synchronizationStrategy) {
		this.indexingPlanSynchronizationStrategy =
				synchronizationStrategyHolder.configureOverriddenSynchronizationStrategy( synchronizationStrategy );
	}

	@Override
	public PojoIndexer createIndexer() {
		return mappingContext.createIndexer( this );
	}

	@Override
	public PojoRuntimeIntrospector runtimeIntrospector() {
		return mappingContext.runtimeIntrospector();
	}

	@Override
	public <T> SearchQuerySelectStep<?, EntityReference, T, ?, ?, ?> search(Collection<? extends Class<? extends T>> classes) {
		return search( scope( classes ) );
	}

	@Override
	public <T> SearchQuerySelectStep<?, EntityReference, T, ?, ?, ?> search(SearchScope<T> scope) {
		return search( (SearchScopeImpl<T>) scope );
	}

	@Override
	public SearchSchemaManager schemaManager(Collection<? extends Class<?>> classes) {
		return scope( classes ).schemaManager();
	}

	@Override
	public SearchWorkspace workspace(Collection<? extends Class<?>> classes) {
		return scope( classes ).workspace( tenantIdentifierValue() );
	}

	@Override
	public <T> SearchScopeImpl<T> scope(Collection<? extends Class<? extends T>> types) {
		return mappingContext.createScope( types );
	}

	@Override
	public <T> SearchScopeImpl<T> scope(Class<T> expectedSuperType, Collection<String> entityNames) {
		return mappingContext.createScope( expectedSuperType, entityNames );
	}

	@Override
	public SearchIndexingPlan indexingPlan() {
		if ( indexingPlan == null ) {
			indexingPlan = new SearchIndexingPlanImpl(
					typeContextProvider, runtimeIntrospector(),
					mappingContext().createIndexingPlan(
							this,
							indexingPlanSynchronizationStrategy.documentCommitStrategy(),
							indexingPlanSynchronizationStrategy.documentRefreshStrategy()
					),
					indexingPlanSynchronizationStrategy
			);
		}
		return indexingPlan;
	}

	@Override
	public SearchIndexer indexer() {
		if ( indexer == null ) {
			indexer = new SearchIndexerImpl(
					runtimeIntrospector(),
					mappingContext().createIndexer( this ),
					indexingPlanSynchronizationStrategy.documentCommitStrategy(),
					indexingPlanSynchronizationStrategy.documentRefreshStrategy()
			);
		}
		return indexer;
	}

	@Override
	public PojoSelectionLoadingContext defaultLoadingContext() {
		return loadingContextBuilder().build();
	}

	@Override
	public ConfiguredSearchIndexingPlanFilter configuredIndexingPlanFilter() {
		return configuredIndexingPlanFilter;
	}

	@Override
	public StandalonePojoSearchSessionMappingContext mappingContext() {
		return mappingContext;
	}

	private <T> SearchQuerySelectStep<?, EntityReference, T, ?, ?, ?> search(SearchScopeImpl<T> scope) {
		return scope.search( this, loadingContextBuilder() );
	}

	private StandalonePojoSelectionLoadingContextBuilder loadingContextBuilder() {
		StandalonePojoLoadingContext.Builder builder = mappingContext.loadingContextBuilder();
		if ( loadingOptionsContributor != null ) {
			loadingOptionsContributor.accept( builder );
		}
		return builder;
	}

	public static class Builder implements SearchSessionBuilder {
		private final StandalonePojoSearchSessionMappingContext mappingContext;
		private final StandalonePojoSearchSessionTypeContextProvider typeContextProvider;
		private final ConfiguredIndexingPlanSynchronizationStrategyHolder synchronizationStrategyHolder;
		private IndexingPlanSynchronizationStrategy synchronizationStrategy;

		private String tenantId;
		private Consumer<SelectionLoadingOptionsStep> loadingOptionsContributor;

		public Builder(StandalonePojoSearchSessionMappingContext mappingContext,
				ConfiguredIndexingPlanSynchronizationStrategyHolder synchronizationStrategyHolder,
				StandalonePojoSearchSessionTypeContextProvider typeContextProvider) {
			this.synchronizationStrategyHolder = synchronizationStrategyHolder;
			this.mappingContext = mappingContext;
			this.typeContextProvider = typeContextProvider;
		}

		@Override
		@SuppressWarnings("removal")
		public Builder tenantId(String tenantId) {
			this.tenantId = tenantId;
			return this;
		}

		@Override
		public SearchSessionBuilder tenantId(Object tenantId) {
			this.tenantId = mappingContext.tenancyConfiguration().convert( tenantId );
			return this;
		}

		@Override
		public SearchSessionBuilder indexingPlanSynchronizationStrategy(
				IndexingPlanSynchronizationStrategy synchronizationStrategy) {
			this.synchronizationStrategy = synchronizationStrategy;
			return this;
		}

		@Override
		public SearchSessionBuilder loading(Consumer<SelectionLoadingOptionsStep> loadingOptionsContributor) {
			this.loadingOptionsContributor = loadingOptionsContributor;
			return this;
		}

		@Override
		public StandalonePojoSearchSession build() {
			return new StandalonePojoSearchSession( this );
		}
	}
}
