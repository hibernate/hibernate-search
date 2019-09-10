/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.transaction.Synchronization;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.loading.spi.ReferenceHitMapper;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.impl.HibernateOrmMapping;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeSessionContext;
import org.hibernate.search.mapper.orm.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.orm.search.query.dsl.HibernateOrmSearchQueryHitTypeStep;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.session.SearchSessionWritePlan;
import org.hibernate.search.mapper.orm.session.context.impl.HibernateOrmSessionContextImpl;
import org.hibernate.search.mapper.orm.writing.SearchWriter;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.session.spi.AbstractPojoSearchSession;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.TransientReference;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * The actual implementation of {@link SearchSession}.
 */
public class HibernateOrmSearchSession extends AbstractPojoSearchSession
		implements SearchSession, HibernateOrmScopeSessionContext, ReferenceHitMapper<EntityReference> {

	/**
	 * @param sessionImplementor A Hibernate session
	 *
	 * @return The {@link HibernateOrmSearchSession} to use within the context of the given session.
	 */
	@SuppressWarnings("unchecked")
	public static HibernateOrmSearchSession get(HibernateOrmSearchSessionMappingContext context,
			SessionImplementor sessionImplementor) {
		checkOrmSessionIsOpen( sessionImplementor );
		TransientReference<HibernateOrmSearchSession> reference =
				(TransientReference<HibernateOrmSearchSession>) sessionImplementor.getProperties()
						.get( SEARCH_SESSION_KEY );
		@SuppressWarnings("resource") // The listener below handles closing
				HibernateOrmSearchSession searchSession = reference == null ? null : reference.get();
		if ( searchSession == null ) {
			searchSession = context.createSessionBuilder( sessionImplementor ).build();
			reference = new TransientReference<>( searchSession );
			sessionImplementor.setProperty( SEARCH_SESSION_KEY, reference );

			// Make sure we will ultimately close the query manager
			sessionImplementor.getEventListenerManager()
					.addListener( new SearchSessionClosingListener( sessionImplementor ) );
		}
		return searchSession;
	}

	private static final String SEARCH_SESSION_KEY =
			HibernateOrmMapping.class.getName() + "#SEARCH_SESSION_KEY";

	private static final String WORK_PLAN_PER_TRANSACTION_MAP_KEY =
			HibernateOrmSearchSession.class.getName() + "#WORK_PLAN_PER_TRANSACTION_KEY";

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmSearchSessionMappingContext mappingContext;
	private final HibernateOrmSessionTypeContextProvider typeContextProvider;
	private final HibernateOrmSessionContextImpl sessionContext;
	private AutomaticIndexingSynchronizationStrategy synchronizationStrategy;

	/*
	 * FIXME HSEARCH-3317 support "enlist in transaction"? This only makes sense when index managers support it,
	 *  maybe there's something to change here...
	 */
	private boolean enlistInTransaction = false;

	private SearchSessionWritePlanImpl writePlan;

	private HibernateOrmSearchSession(HibernateOrmSearchSessionBuilder builder) {
		this( builder, builder.buildBackendSessionContext() );
	}

	private HibernateOrmSearchSession(HibernateOrmSearchSessionBuilder builder,
			HibernateOrmSessionContextImpl backendSessionContext) {
		super( builder, backendSessionContext );
		this.mappingContext = builder.mappingContext;
		this.typeContextProvider = builder.typeContextProvider;
		this.sessionContext = backendSessionContext;
		this.synchronizationStrategy = builder.synchronizationStrategy;
	}

	public void close() {
		// Nothing to do
	}

	@Override
	public <T> HibernateOrmSearchQueryHitTypeStep<T> search(Collection<? extends Class<? extends T>> types) {
		return createScope( types ).search( this );
	}

	@Override
	public SearchWriter writer(Collection<? extends Class<?>> types) {
		return createScope( types ).writer( this );
	}

	@Override
	public MassIndexer massIndexer(Collection<? extends Class<?>> types) {
		return createScope( types ).massIndexer( this );
	}

	@Override
	public EntityManager toEntityManager() {
		return sessionContext.getSession();
	}

	@Override
	public Session toOrmSession() {
		return sessionContext.getSession();
	}

	@Override
	public SearchSessionWritePlan writePlan() {
		if ( writePlan == null ) {
			writePlan = new SearchSessionWritePlanImpl( this );
		}
		return writePlan;
	}

	@Override
	public void setAutomaticIndexingSynchronizationStrategy(
			AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		this.synchronizationStrategy = synchronizationStrategy;
	}

	@Override
	public SessionImplementor getSession() {
		return sessionContext.getSession();
	}

	@Override
	public BackendSessionContext getBackendSessionContext() {
		return sessionContext;
	}

	@Override
	public DetachedBackendSessionContext getDetachedBackendSessionContext() {
		return DetachedBackendSessionContext.of( sessionContext );
	}

	@Override
	public ReferenceHitMapper<EntityReference> getReferenceHitMapper() {
		return this;
	}

	@Override
	public EntityReference fromDocumentReference(DocumentReference reference) {
		HibernateOrmSessionIndexedTypeContext<?> typeContext =
				typeContextProvider.getByIndexName( reference.getIndexName() );
		if ( typeContext == null ) {
			throw new AssertionFailure(
					"Document reference " + reference + " refers to an unknown index"
			);
		}
		Object id = typeContext.getIdentifierMapping()
				.fromDocumentIdentifier( reference.getId(), sessionContext );
		return new EntityReferenceImpl( typeContext.getJavaClass(), id );
	}

	public PojoSessionWorkExecutor createSessionWorkExecutor(DocumentCommitStrategy commitStrategy) {
		return getDelegate().createSessionWorkExecutor( commitStrategy );
	}

	@SuppressWarnings("unchecked")
	public PojoWorkPlan getCurrentWorkPlan(boolean createIfDoesNotExist) {
		SessionImplementor sessionImplementor = sessionContext.getSession();
		Transaction transactionIdentifier = null;

		TransientReference<Map<Transaction, PojoWorkPlan>> reference = (TransientReference<Map<Transaction, PojoWorkPlan>>) sessionImplementor.getProperties()
				.get( WORK_PLAN_PER_TRANSACTION_MAP_KEY );
		Map<Transaction, PojoWorkPlan> workPlanPerTransaction = reference == null ? null : reference.get();
		if ( workPlanPerTransaction == null ) {
			workPlanPerTransaction = new HashMap<>();
			reference = new TransientReference<>( workPlanPerTransaction );
			sessionImplementor.setProperty( WORK_PLAN_PER_TRANSACTION_MAP_KEY, reference );
		}

		if ( sessionImplementor.isTransactionInProgress() ) {
			transactionIdentifier = sessionImplementor.accessTransaction();
		}
		// For out of transaction case we will use null as transaction identifier

		PojoWorkPlan workPlan = workPlanPerTransaction.get( transactionIdentifier );
		if ( workPlan != null ) {
			return workPlan;
		}

		if ( !createIfDoesNotExist ) {
			return null;
		}

		AutomaticIndexingSynchronizationStrategy workPlanSynchronizationStrategy = synchronizationStrategy;
		workPlan = createWorkPlan(
				workPlanSynchronizationStrategy.getDocumentCommitStrategy(),
				workPlanSynchronizationStrategy.getDocumentRefreshStrategy()
		);
		workPlanPerTransaction.put( transactionIdentifier, workPlan );

		if ( sessionImplementor.isTransactionInProgress() ) {
			Synchronization txSync = createTransactionWorkQueueSynchronization(
					workPlan, workPlanPerTransaction, transactionIdentifier, workPlanSynchronizationStrategy
			);
			registerSynchronization( sessionImplementor, txSync );
		}
		return workPlan;
	}

	AutomaticIndexingSynchronizationStrategy getAutomaticIndexingSynchronizationStrategy() {
		return synchronizationStrategy;
	}

	private <T> SearchScopeImpl<T> createScope(Collection<? extends Class<? extends T>> types) {
		checkOrmSessionIsOpen();
		return mappingContext.createScope( types );
	}

	private PojoWorkPlan createWorkPlan(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return getDelegate().createWorkPlan( commitStrategy, refreshStrategy );
	}

	private Synchronization createTransactionWorkQueueSynchronization(PojoWorkPlan workPlan,
			Map<Transaction, PojoWorkPlan> workPlanPerTransaction, Transaction transactionIdentifier,
			AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		if ( enlistInTransaction ) {
			return new InTransactionWorkQueueSynchronization(
					workPlan, workPlanPerTransaction, transactionIdentifier,
					synchronizationStrategy
			);
		}
		else {
			return new PostTransactionWorkQueueSynchronization(
					workPlan, workPlanPerTransaction, transactionIdentifier,
					synchronizationStrategy
			);
		}
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

	void checkOrmSessionIsOpen() {
		checkOrmSessionIsOpen( sessionContext.getSession() );
	}

	static void checkOrmSessionIsOpen(SessionImplementor session) {
		try {
			session.checkOpen();
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionIsClosed( e );
		}
	}

	public static class HibernateOrmSearchSessionBuilder extends AbstractBuilder<HibernateOrmSearchSession> {
		private final HibernateOrmSearchSessionMappingContext mappingContext;
		private final HibernateOrmSessionTypeContextProvider typeContextProvider;
		private final SessionImplementor sessionImplementor;
		private final AutomaticIndexingSynchronizationStrategy synchronizationStrategy;

		public HibernateOrmSearchSessionBuilder(PojoMappingDelegate mappingDelegate,
				HibernateOrmSearchSessionMappingContext mappingContext,
				HibernateOrmSessionTypeContextProvider typeContextProvider,
				SessionImplementor sessionImplementor,
				AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
			super( mappingDelegate );
			this.mappingContext = mappingContext;
			this.typeContextProvider = typeContextProvider;
			this.sessionImplementor = sessionImplementor;
			this.synchronizationStrategy = synchronizationStrategy;
		}

		private HibernateOrmSessionContextImpl buildBackendSessionContext() {
			return new HibernateOrmSessionContextImpl( mappingContext.getBackendMappingContext(), sessionImplementor );
		}

		@Override
		public HibernateOrmSearchSession build() {
			return new HibernateOrmSearchSession( this );
		}
	}

	private static class SearchSessionClosingListener extends BaseSessionEventListener {
		private final SessionImplementor sessionImplementor;

		private SearchSessionClosingListener(SessionImplementor sessionImplementor) {
			this.sessionImplementor = sessionImplementor;
		}

		@Override
		public void end() {
			@SuppressWarnings("unchecked") // This key "belongs" to us, we know what we put in there.
					TransientReference<HibernateOrmSearchSession> reference =
					(TransientReference<HibernateOrmSearchSession>) sessionImplementor.getProperties()
							.get( SEARCH_SESSION_KEY );
			HibernateOrmSearchSession searchSession = reference == null ? null : reference.get();
			if ( searchSession != null ) {
				searchSession.close();
			}
		}
	}
}
