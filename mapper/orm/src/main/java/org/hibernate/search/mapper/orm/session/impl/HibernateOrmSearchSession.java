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

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.mapper.session.context.spi.DetachedSessionContextImplementor;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeIndexedTypeContext;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeMappingContext;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeSessionContext;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeTypeContextProvider;
import org.hibernate.search.mapper.orm.search.SearchScope;
import org.hibernate.search.mapper.orm.scope.impl.SearchScopeImpl;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.mapper.orm.session.SearchSessionWritePlan;
import org.hibernate.search.mapper.orm.mapping.context.impl.HibernateOrmMappingContextImpl;
import org.hibernate.search.mapper.orm.session.context.impl.HibernateOrmSessionContextImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.session.spi.AbstractPojoSearchSession;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;
import org.hibernate.search.util.common.impl.TransientReference;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * The actual implementation of {@link SearchSession}.
 */
public class HibernateOrmSearchSession extends AbstractPojoSearchSession
		implements SearchSession, HibernateOrmScopeSessionContext {

	private static final String WORK_PLAN_PER_TRANSACTION_MAP_KEY =
			HibernateOrmSearchSession.class.getName() + "#WORK_PLAN_PER_TRANSACTION_KEY";

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmScopeMappingContext scopeMappingContext;
	private final HibernateOrmScopeTypeContextProvider typeContextProvider;
	private final HibernateOrmSessionContextImpl sessionContext;
	private AutomaticIndexingSynchronizationStrategy synchronizationStrategy;

	/*
	 * FIXME HSEARCH-3317 support "enlist in transaction"? This only makes sense when index managers support it,
	 *  maybe there's something to change here...
	 */
	private boolean enlistInTransaction = false;

	private SearchSessionWritePlanImpl writePlan;

	private HibernateOrmSearchSession(HibernateOrmSearchSessionBuilder builder) {
		this( builder, builder.buildSessionContext() );
	}

	private HibernateOrmSearchSession(HibernateOrmSearchSessionBuilder builder,
			HibernateOrmSessionContextImpl sessionContext) {
		super( builder, sessionContext );
		this.scopeMappingContext = builder.scopeMappingContext;
		this.typeContextProvider = builder.typeContextProvider;
		this.sessionContext = sessionContext;
		this.synchronizationStrategy = builder.synchronizationStrategy;
	}

	public void close() {
		// Nothing to do
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
	public <T> SearchScope<T> scope(Collection<? extends Class<? extends T>> types) {
		checkOrmSessionIsOpen();

		PojoScopeDelegate<T, HibernateOrmScopeIndexedTypeContext<? extends T>> scopeDelegate =
				getDelegate().createPojoScope(
						types,
						typeContextProvider::getIndexedByExactClass
				);
		return new SearchScopeImpl<>( scopeMappingContext, this, scopeDelegate );
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
	public DetachedSessionContextImplementor getDetachedSessionContext() {
		return DetachedSessionContextImplementor.of( sessionContext );
	}

	public PojoSessionWorkExecutor createSessionWorkExecutor(DocumentCommitStrategy commitStrategy) {
		return getDelegate().createSessionWorkExecutor( commitStrategy );
	}

	@SuppressWarnings("unchecked")
	public PojoWorkPlan getCurrentWorkPlan() {
		SessionImplementor sessionImplementor = sessionContext.getSession();
		if ( sessionImplementor.isTransactionInProgress() ) {
			final Transaction transactionIdentifier = sessionImplementor.accessTransaction();
			TransientReference<Map<Transaction, PojoWorkPlan>> reference =
					(TransientReference<Map<Transaction, PojoWorkPlan>>) sessionImplementor.getProperties()
							.get( WORK_PLAN_PER_TRANSACTION_MAP_KEY );
			Map<Transaction, PojoWorkPlan> workPlanPerTransaction = reference == null ? null : reference.get();
			if ( workPlanPerTransaction == null ) {
				workPlanPerTransaction = new HashMap<>();
				reference = new TransientReference<>( workPlanPerTransaction );
				sessionImplementor.setProperty( WORK_PLAN_PER_TRANSACTION_MAP_KEY, reference );
			}
			PojoWorkPlan workPlan = workPlanPerTransaction.get( transactionIdentifier );
			if ( workPlan == null ) {
				AutomaticIndexingSynchronizationStrategy workPlanSynchronizationStrategy =
						synchronizationStrategy;
				workPlan = createWorkPlan(
						workPlanSynchronizationStrategy.getDocumentCommitStrategy(),
						workPlanSynchronizationStrategy.getDocumentRefreshStrategy()
				);
				workPlanPerTransaction.put( transactionIdentifier, workPlan );
				Synchronization txSync = createTransactionWorkQueueSynchronization(
						workPlan, workPlanPerTransaction, transactionIdentifier, workPlanSynchronizationStrategy
				);
				registerSynchronization( sessionImplementor, txSync );
			}
			return workPlan;
		}
		/*
		 * TODO HSEARCH-3068 handle the "simulated" transaction when "a Flush listener is registered".
		 * See:
		 *  - HibernateSearchEventListener (in Search 5 and here)
		 *  - the else block in org.hibernate.search.event.impl.EventSourceTransactionContext#registerSynchronization in Search 5
		else if ( some condition ) {
			throw new UnsupportedOperationException( "Not implemented yet" );
		}
		 */
		else {
			// TODO HSEARCH-3069 add a warning when configuration expects transactions, but none was found
//			if ( transactionExpected ) {
//				// this is a workaround: isTransactionInProgress should return "true"
//				// for correct configurations.
//				log.pushedChangesOutOfTransaction();
//			}
			// TODO HSEARCH-3069 Create a work plan (to handle automatic reindexing of containing types),
			//  but ensure changes will be applied without waiting for a call to workPlan.execute()
			// TODO HSEARCH-3069 also ensure synchronicity if necessary (make some Session event, such as flush(), wait for the works to be executed)
			throw new UnsupportedOperationException( "Not implemented yet" );
		}
	}

	AutomaticIndexingSynchronizationStrategy getAutomaticIndexingSynchronizationStrategy() {
		return synchronizationStrategy;
	}

	private PojoWorkPlan createWorkPlan(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return getDelegate().createWorkPlan( commitStrategy, refreshStrategy );
	}

	private Synchronization createTransactionWorkQueueSynchronization(PojoWorkPlan workPlan,
			Map<Transaction, PojoWorkPlan> workPlanPerTransaction, Object transactionIdentifier,
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
		try {
			sessionContext.getSession().checkOpen();
		}
		catch (IllegalStateException e) {
			throw log.hibernateSessionIsClosed( e );
		}
	}

	public static class HibernateOrmSearchSessionBuilder extends AbstractBuilder<HibernateOrmSearchSession> {
		private final HibernateOrmMappingContextImpl mappingContext;
		private final HibernateOrmScopeMappingContext scopeMappingContext;
		private final HibernateOrmScopeTypeContextProvider typeContextProvider;
		private final SessionImplementor sessionImplementor;
		private final AutomaticIndexingSynchronizationStrategy synchronizationStrategy;

		public HibernateOrmSearchSessionBuilder(PojoMappingDelegate mappingDelegate,
				HibernateOrmMappingContextImpl mappingContext,
				HibernateOrmScopeMappingContext scopeMappingContext,
				HibernateOrmScopeTypeContextProvider typeContextProvider,
				SessionImplementor sessionImplementor,
				AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
			super( mappingDelegate );
			this.mappingContext = mappingContext;
			this.scopeMappingContext = scopeMappingContext;
			this.typeContextProvider = typeContextProvider;
			this.sessionImplementor = sessionImplementor;
			this.synchronizationStrategy = synchronizationStrategy;
		}

		private HibernateOrmSessionContextImpl buildSessionContext() {
			return new HibernateOrmSessionContextImpl( mappingContext, sessionImplementor );
		}

		@Override
		public HibernateOrmSearchSession build() {
			return new HibernateOrmSearchSession( this );
		}
	}
}
