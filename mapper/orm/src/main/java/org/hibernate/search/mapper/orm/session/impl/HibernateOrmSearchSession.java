/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.session.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.asSetIgnoreNull;

import java.util.Collection;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Synchronization;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.search.common.NonStaticMetamodelScope;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.mapper.orm.automaticindexing.session.impl.DelegatingAutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingEventSendingSessionContext;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmSelectionLoadingContext;
import org.hibernate.search.mapper.orm.logging.impl.ConfigurationLog;
import org.hibernate.search.mapper.orm.logging.impl.OrmMiscLog;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmRuntimeIntrospector;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.orm.scope.HibernateOrmRootReferenceScope;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.scope.TypedSearchScope;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeSessionContext;
import org.hibernate.search.mapper.orm.scope.impl.TypedSearchScopeImpl;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.session.context.HibernateOrmSessionContext;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.mapper.orm.work.impl.SearchIndexingPlanImpl;
import org.hibernate.search.mapper.orm.work.impl.SearchIndexingPlanSessionContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRuntimeIntrospector;
import org.hibernate.search.mapper.pojo.session.spi.AbstractPojoSearchSession;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanFilter;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredSearchIndexingPlanFilter;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventProcessingPlan;

/**
 * The actual implementation of {@link SearchSession}.
 */
@SuppressWarnings("deprecation")
public class HibernateOrmSearchSession extends AbstractPojoSearchSession
		implements SearchSession, HibernateOrmSessionContext, HibernateOrmScopeSessionContext,
		SearchIndexingPlanSessionContext,
		AutomaticIndexingEventSendingSessionContext {

	/**
	 * @param sessionImplementor A Hibernate session
	 * @return The {@link HibernateOrmSearchSession} to use within the context of the given session.
	 */
	public static HibernateOrmSearchSession get(HibernateOrmSearchSessionMappingContext context,
			SessionImplementor sessionImplementor) {
		return get( context, sessionImplementor, true );
	}

	/**
	 * @param sessionImplementor A Hibernate session
	 * @return The {@link HibernateOrmSearchSession} to use within the context of the given session.
	 */
	public static HibernateOrmSearchSession get(HibernateOrmSearchSessionMappingContext context,
			SessionImplementor sessionImplementor, boolean createIfDoesNotExist) {
		HibernateOrmSearchSessionHolder holder =
				HibernateOrmSearchSessionHolder.get( sessionImplementor, createIfDoesNotExist );
		if ( holder == null ) {
			// Can only happen if createIfDoesNotExist is false
			return null;
		}
		HibernateOrmSearchSession searchSession = holder.searchSession();
		if ( searchSession != null ) {
			return searchSession;
		}

		if ( !createIfDoesNotExist ) {
			return null;
		}

		searchSession = context.createSessionBuilder( sessionImplementor ).build();
		holder.searchSession( searchSession );
		return searchSession;
	}

	private final HibernateOrmSearchSessionMappingContext mappingContext;
	private final HibernateOrmSessionTypeContextProvider typeContextProvider;
	private final SessionImplementor sessionImplementor;
	private final HibernateOrmRuntimeIntrospector runtimeIntrospector;
	private final ConfiguredAutomaticIndexingStrategy automaticIndexingStrategy;
	private ConfiguredSearchIndexingPlanFilter configuredIndexingPlanFilter;
	private ConfiguredIndexingPlanSynchronizationStrategy indexingPlanSynchronizationStrategy;

	private SearchIndexingPlanImpl indexingPlan;

	private HibernateOrmSearchSession(Builder builder) {
		super( builder.mappingContext );
		this.mappingContext = builder.mappingContext;
		this.typeContextProvider = builder.typeContextProvider;
		this.automaticIndexingStrategy = builder.automaticIndexingStrategy;
		this.sessionImplementor = builder.sessionImplementor;
		this.runtimeIntrospector = builder.buildRuntimeIntrospector();
		// make sure that even if a session filter is not configured we will fall back to an application one if needed.
		this.configuredIndexingPlanFilter = mappingContext.applicationIndexingPlanFilter();
		this.indexingPlanSynchronizationStrategy = automaticIndexingStrategy.defaultIndexingPlanSynchronizationStrategy();
	}

	@Override
	public HibernateOrmSearchSessionMappingContext mappingContext() {
		return mappingContext;
	}

	@Override
	public String tenantIdentifier() {
		return session().getTenantIdentifier();
	}

	@Override
	public Object tenantIdentifierValue() {
		return session().getTenantIdentifierValue();
	}

	@Override
	public PojoIndexer createIndexer() {
		return mappingContext.createIndexer( this );
	}

	@Override
	public <T> SearchQuerySelectStep<NonStaticMetamodelScope, ?, EntityReference, T, SearchLoadingOptionsStep, ?, ?> search(
			Collection<? extends Class<? extends T>> classes) {
		return search( scope( classes ) );
	}

	@Override
	public <SR, T> SearchQuerySelectStep<SR, ?, EntityReference, T, SearchLoadingOptionsStep, ?, ?> search(
			TypedSearchScope<SR, T> scope) {
		return search( (TypedSearchScopeImpl<SR, T>) scope );
	}

	@Override
	public <T> SearchQuerySelectStep<?, ?, EntityReference, T, SearchLoadingOptionsStep, ?, ?> search(SearchScope<T> scope) {
		return search( (TypedSearchScopeImpl<?, T>) scope );
	}

	@Override
	public <SR, T> SearchQuerySelectStep<SR, ?, EntityReference, T, SearchLoadingOptionsStep, ?, ?> search(
			HibernateOrmRootReferenceScope<SR, T> scope) {
		return search( ( scope.scope( this ) ) );
	}

	private <SR, T> SearchQuerySelectStep<SR,
			?,
			EntityReference,
			T,
			SearchLoadingOptionsStep,
			?,
			?> search(TypedSearchScopeImpl<SR, T> scope) {
		return scope.search( this, loadingContextBuilder() );
	}

	@Override
	public SearchSchemaManager schemaManager(Collection<? extends Class<?>> classes) {
		return scope( classes ).schemaManager();
	}

	@Override
	public SearchWorkspace workspace(Collection<? extends Class<?>> classes) {
		return scope( classes ).workspace( tenantIdentifier() );
	}

	@Override
	public MassIndexer massIndexer(Collection<? extends Class<?>> classes) {
		return scope( classes ).massIndexer( asSetIgnoreNull( tenantIdentifier() ) );
	}

	@Override
	public <T> TypedSearchScopeImpl<NonStaticMetamodelScope, T> scope(Collection<? extends Class<? extends T>> classes) {
		checkOpen();
		return mappingContext.createScope( NonStaticMetamodelScope.class, classes );
	}

	@Override
	public <T> TypedSearchScope<NonStaticMetamodelScope, T> scope(Class<T> expectedSuperType, Collection<String> entityNames) {
		checkOpen();
		return mappingContext.createScope( NonStaticMetamodelScope.class, expectedSuperType, entityNames );
	}

	@Override
	public <SR, T> TypedSearchScope<SR, T> typedScope(Class<SR> rootScope, Collection<? extends Class<? extends T>> classes) {
		return mappingContext.createScope( rootScope, classes );
	}

	@Override
	public EntityManager toEntityManager() {
		return sessionImplementor;
	}

	@Override
	public Session toOrmSession() {
		return sessionImplementor;
	}

	@Override
	public SearchIndexingPlan indexingPlan() {
		if ( indexingPlan == null ) {
			indexingPlan = new SearchIndexingPlanImpl( typeContextProvider, this );
		}
		return indexingPlan;
	}

	@Override
	@SuppressWarnings("deprecation") // need to keep OLD API still implemented
	public void automaticIndexingSynchronizationStrategy(
			org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		indexingPlanSynchronizationStrategy(
				synchronizationStrategy instanceof DelegatingAutomaticIndexingSynchronizationStrategy
						? ( (DelegatingAutomaticIndexingSynchronizationStrategy) synchronizationStrategy ).delegate()
						: new HibernateOrmIndexingPlanSynchronizationStrategyAdapter( synchronizationStrategy )
		);
	}

	@Override
	public void indexingPlanSynchronizationStrategy(IndexingPlanSynchronizationStrategy synchronizationStrategy) {
		this.indexingPlanSynchronizationStrategy =
				automaticIndexingStrategy.configureOverriddenSynchronizationStrategy( synchronizationStrategy );
	}

	@Override
	public void indexingPlanFilter(SearchIndexingPlanFilter filter) {
		ConfiguredSearchIndexingPlanFilter configuredFilter = mappingContext.configuredSearchIndexingPlanFilter(
				filter );

		if ( automaticIndexingStrategy.usesAsyncProcessing() && !configuredFilter.supportsAsyncProcessing() ) {
			throw ConfigurationLog.INSTANCE.cannotApplySessionFilterWhenAsyncProcessingIsUsed();
		}
		configuredIndexingPlanFilter = configuredFilter;
	}

	@Override
	public ConfiguredSearchIndexingPlanFilter configuredIndexingPlanFilter() {
		return configuredIndexingPlanFilter;
	}

	@Override
	public SessionImplementor session() {
		return sessionImplementor;
	}

	@Override
	public EntityReferenceFactory entityReferenceFactory() {
		return mappingContext.entityReferenceFactory();
	}

	@Override
	public PojoSelectionLoadingContext defaultLoadingContext() {
		return loadingContextBuilder().build();
	}

	@Override
	public PojoRuntimeIntrospector runtimeIntrospector() {
		return runtimeIntrospector;
	}

	@Override
	public PojoIndexingPlan currentIndexingPlan(boolean createIfDoesNotExist) {
		HibernateOrmSearchSessionHolder holder =
				HibernateOrmSearchSessionHolder.get( sessionImplementor, createIfDoesNotExist );
		if ( holder == null ) {
			// Can only happen if createIfDoesNotExist is false
			return null;
		}

		Transaction transactionIdentifier;
		if ( sessionImplementor.isTransactionInProgress() ) {
			transactionIdentifier = sessionImplementor.accessTransaction();
		}
		else {
			transactionIdentifier = null;
		}

		PojoIndexingPlan plan = holder.pojoIndexingPlan( transactionIdentifier );
		if ( plan != null ) {
			return plan;
		}

		if ( !createIfDoesNotExist ) {
			return null;
		}

		ConfiguredIndexingPlanSynchronizationStrategy currentSynchronizationStrategy =
				indexingPlanSynchronizationStrategy;
		plan = automaticIndexingStrategy.createIndexingPlan( this, currentSynchronizationStrategy );
		holder.pojoIndexingPlan( transactionIdentifier, plan );

		if ( sessionImplementor.isTransactionInProgress() ) {
			Synchronization txSync = automaticIndexingStrategy.createTransactionWorkQueueSynchronization(
					plan, holder, transactionIdentifier,
					currentSynchronizationStrategy
			);
			registerSynchronization( sessionImplementor, txSync );
		}
		return plan;
	}

	public PojoIndexingQueueEventProcessingPlan createIndexingQueueEventProcessingPlan() {
		return automaticIndexingStrategy
				.createIndexingQueueEventProcessingPlan( this, indexingPlanSynchronizationStrategy );
	}

	@Override
	public void checkOpen() {
		checkOpen( sessionImplementor );
	}

	@Override
	public ConfiguredIndexingPlanSynchronizationStrategy configuredAutomaticIndexingSynchronizationStrategy() {
		return indexingPlanSynchronizationStrategy;
	}

	private HibernateOrmSelectionLoadingContext.Builder loadingContextBuilder() {
		return new HibernateOrmSelectionLoadingContext.Builder( mappingContext, this );
	}

	private void registerSynchronization(SessionImplementor sessionImplementor, Synchronization synchronization) {
		//use {Before|After}TransactionCompletionProcess instead of registerSynchronization because it does not
		//swallow transactions.
		/*
		 * HSEARCH-540: the pre process must be both a BeforeTransactionCompletionProcess and a TX Synchronization.
		 *
		 * In a resource-local tx env, the beforeCommit phase is called after the flush, and prepares work queue.
		 * Also, any exceptions that occur during that are propagated (if a Synchronization was used, the exceptions
		 * would be eaten).
		 *
		 * In a JTA env, the before transaction completion is called before the flush, so not all changes are yet
		 * written. However, Synchronization-s do propagate exceptions, so they can be safely used.
		 */
		final ActionQueue actionQueue = sessionImplementor.getActionQueue();
		SynchronizationAdapter adapter = new SynchronizationAdapter( synchronization );

		boolean isLocal = isLocalTransaction( sessionImplementor );
		if ( isLocal ) {
			//if local tx never use Synchronization
			actionQueue.registerProcess( (BeforeTransactionCompletionProcess) adapter );
		}
		else {
			//TODO could we remove the action queue registration in this case?
			actionQueue.registerProcess( (BeforeTransactionCompletionProcess) adapter );
			sessionImplementor.accessTransaction().registerSynchronization( adapter );
		}

		//executed in all environments
		actionQueue.registerProcess( (AfterTransactionCompletionProcess) adapter );
	}

	private boolean isLocalTransaction(SessionImplementor sessionImplementor) {
		return !sessionImplementor
				.getTransactionCoordinator()
				.getTransactionCoordinatorBuilder()
				.isJta();
	}

	private static void checkOpen(SessionImplementor session) {
		try {
			session.checkOpen();
		}
		catch (IllegalStateException e) {
			throw OrmMiscLog.INSTANCE.hibernateSessionIsClosed( e );
		}
	}

	public static class Builder {
		private final HibernateOrmSearchSessionMappingContext mappingContext;
		private final HibernateOrmSessionTypeContextProvider typeContextProvider;
		private final ConfiguredAutomaticIndexingStrategy automaticIndexingStrategy;
		private final SessionImplementor sessionImplementor;

		public Builder(HibernateOrmSearchSessionMappingContext mappingContext,
				HibernateOrmSessionTypeContextProvider typeContextProvider,
				ConfiguredAutomaticIndexingStrategy automaticIndexingStrategy,
				SessionImplementor sessionImplementor) {
			this.mappingContext = mappingContext;
			this.typeContextProvider = typeContextProvider;
			this.automaticIndexingStrategy = automaticIndexingStrategy;
			this.sessionImplementor = sessionImplementor;
		}

		private HibernateOrmRuntimeIntrospector buildRuntimeIntrospector() {
			return new HibernateOrmRuntimeIntrospector( mappingContext.typeIdentifierResolver(), sessionImplementor );
		}

		public HibernateOrmSearchSession build() {
			return new HibernateOrmSearchSession( this );
		}
	}

}
