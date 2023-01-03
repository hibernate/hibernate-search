/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import javax.persistence.EntityManager;
import javax.transaction.Synchronization;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.common.spi.DocumentReferenceConverter;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.automaticindexing.session.impl.ConfiguredAutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.automaticindexing.spi.AutomaticIndexingEventSendingSessionContext;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.orm.loading.impl.HibernateOrmSelectionLoadingContext;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.model.impl.HibernateOrmRuntimeIntrospector;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeSessionContext;
import org.hibernate.search.mapper.orm.scope.impl.SearchScopeImpl;
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
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingQueueEventProcessingPlan;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;

/**
 * The actual implementation of {@link SearchSession}.
 */
public class HibernateOrmSearchSession extends AbstractPojoSearchSession
		implements SearchSession, HibernateOrmSessionContext, HibernateOrmScopeSessionContext,
				SearchIndexingPlanSessionContext, DocumentReferenceConverter<EntityReference>,
				AutomaticIndexingEventSendingSessionContext {

	/**
	 * @param sessionImplementor A Hibernate session
	 *
	 * @return The {@link HibernateOrmSearchSession} to use within the context of the given session.
	 */
	public static HibernateOrmSearchSession get(HibernateOrmSearchSessionMappingContext context,
			SessionImplementor sessionImplementor) {
		return get( context, sessionImplementor, true );
	}

	/**
	 * @param sessionImplementor A Hibernate session
	 *
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

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmSearchSessionMappingContext mappingContext;
	private final HibernateOrmSessionTypeContextProvider typeContextProvider;
	private final SessionImplementor sessionImplementor;
	private final HibernateOrmRuntimeIntrospector runtimeIntrospector;
	private final ConfiguredAutomaticIndexingStrategy automaticIndexingStrategy;
	private ConfiguredAutomaticIndexingSynchronizationStrategy indexingPlanSynchronizationStrategy;

	private SearchIndexingPlanImpl indexingPlan;

	private HibernateOrmSearchSession(Builder builder) {
		super( builder.mappingContext );
		this.mappingContext = builder.mappingContext;
		this.typeContextProvider = builder.typeContextProvider;
		this.automaticIndexingStrategy = builder.automaticIndexingStrategy;
		this.sessionImplementor = builder.sessionImplementor;
		this.runtimeIntrospector = builder.buildRuntimeIntrospector();
		this.indexingPlanSynchronizationStrategy = automaticIndexingStrategy.defaultIndexingPlanSynchronizationStrategy();
	}

	@Override
	public String tenantIdentifier() {
		return session().getTenantIdentifier();
	}

	@Override
	public PojoIndexer createIndexer() {
		return mappingContext.createIndexer( this );
	}

	@Override
	public <T> SearchQuerySelectStep<?, EntityReference, T, SearchLoadingOptionsStep, ?, ?> search(
			Collection<? extends Class<? extends T>> types) {
		return search( scope( types ) );
	}

	@Override
	public <T> SearchQuerySelectStep<?, EntityReference, T, SearchLoadingOptionsStep, ?, ?> search(
			SearchScope<T> scope) {
		return search( (SearchScopeImpl<T>) scope );
	}

	public <T> SearchQuerySelectStep<?, EntityReference, T, SearchLoadingOptionsStep, ?, ?> search(
			SearchScopeImpl<T> scope) {
		return scope.search( this, loadingContextBuilder() );
	}

	@Override
	public SearchSchemaManager schemaManager(Collection<? extends Class<?>> types) {
		return scope( types ).schemaManager();
	}

	@Override
	public SearchWorkspace workspace(Collection<? extends Class<?>> types) {
		return scope( types ).workspace( tenantIdentifier() );
	}

	@Override
	public MassIndexer massIndexer(Collection<? extends Class<?>> types) {
		return scope( types ).massIndexer( Collections.singleton( tenantIdentifier() ) );
	}

	@Override
	public <T> SearchScopeImpl<T> scope(Collection<? extends Class<? extends T>> types) {
		checkOpen();
		return mappingContext.createScope( types );
	}

	@Override
	public <T> SearchScope<T> scope(Class<T> expectedSuperType, Collection<String> entityNames) {
		checkOpen();
		return mappingContext.createScope( expectedSuperType, entityNames );
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
	public void automaticIndexingSynchronizationStrategy(
			AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		this.indexingPlanSynchronizationStrategy =
				automaticIndexingStrategy.configureOverriddenSynchronizationStrategy( synchronizationStrategy );
	}

	@Override
	public SessionImplementor session() {
		return sessionImplementor;
	}

	@Override
	public DocumentReferenceConverter<EntityReference> documentReferenceConverter() {
		return this;
	}

	@Override
	public EntityReference fromDocumentReference(DocumentReference reference) {
		HibernateOrmSessionTypeContext<?> typeContext =
				typeContextProvider.byJpaEntityName().getOrFail( reference.typeName() );
		Object id = typeContext.identifierMapping()
				.fromDocumentIdentifier( reference.id(), this );
		return new EntityReferenceImpl( typeContext.typeIdentifier(), typeContext.jpaEntityName(), id );
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

		ConfiguredAutomaticIndexingSynchronizationStrategy currentSynchronizationStrategy =
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
	public ConfiguredAutomaticIndexingSynchronizationStrategy configuredAutomaticIndexingSynchronizationStrategy() {
		return indexingPlanSynchronizationStrategy;
	}

	private HibernateOrmSelectionLoadingContext.Builder loadingContextBuilder() {
		return new HibernateOrmSelectionLoadingContext.Builder( mappingContext, typeContextProvider, this );
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
			throw log.hibernateSessionIsClosed( e );
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
			return new HibernateOrmRuntimeIntrospector( typeContextProvider, sessionImplementor );
		}

		public HibernateOrmSearchSession build() {
			return new HibernateOrmSearchSession( this );
		}
	}

}
