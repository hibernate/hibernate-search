/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.BaseSessionEventListener;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.session.spi.SearchSessionImplementor;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.spi.HibernateOrmMapping;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.service.Service;

public class HibernateSearchContextService implements Service, AutoCloseable {

	private static final String SEARCH_SESSION_KEY =
			HibernateSearchContextService.class.getName() + "#SEARCH_SESSION_KEY";

	private static final String WORK_PLAN_PER_TRANSACTION_MAP_KEY =
			HibernateSearchContextService.class.getName() + "#WORK_PLAN_PER_TRANSACTION_KEY";

	private volatile SearchIntegration integration;
	private volatile HibernateOrmMapping mapping;

	/*
	 * FIXME support "enlist in transaction"? This only makes sense when index managers support it,
	 * maybe there's something to change here...
	 */
	private boolean enlistInTransaction = false;

	@Override
	public void close() {
		if ( integration != null ) {
			integration.close();
		}
	}

	public void initialize(SearchIntegration integration, HibernateOrmMapping mapping) {
		this.integration = integration;
		this.mapping = mapping;
	}

	public SearchIntegration getIntegration() {
		if ( integration != null ) {
			return integration;
		}
		else {
			throw LoggerFactory.make( Log.class, MethodHandles.lookup() ).hibernateSearchNotInitialized();
		}
	}

	public HibernateOrmMapping getMapping() {
		if ( mapping != null ) {
			return mapping;
		}
		else {
			throw LoggerFactory.make( Log.class, MethodHandles.lookup() ).hibernateSearchNotInitialized();
		}
	}

	/**
	 * @param sessionImplementor A Hibernate session
	 *
	 * @return The {@link SearchSessionImplementor} to use within the context of the given session.
	 */
	@SuppressWarnings("unchecked")
	public SearchSessionImplementor getSearchSession(SessionImplementor sessionImplementor) {
		TransientReference<SearchSessionImplementor> reference =
				(TransientReference<SearchSessionImplementor>) sessionImplementor.getProperties().get(
						SEARCH_SESSION_KEY );
		@SuppressWarnings("resource") // The listener below handles closing
		SearchSessionImplementor searchSession = reference == null ? null : reference.get();
		if ( searchSession == null ) {
			searchSession = getMapping().createSession( sessionImplementor );
			reference = new TransientReference<>( searchSession );
			sessionImplementor.setProperty( SEARCH_SESSION_KEY, reference );

			// Make sure we will ultimately close the query manager
			sessionImplementor.getEventListenerManager().addListener( new SearchSessionClosingListener( sessionImplementor ) );
		}
		return searchSession;
	}

	/**
	 * @param sessionImplementor A Hibernate session
	 *
	 * @return The {@link PojoWorkPlan} to use for changes to entities in the given session.
	 */
	@SuppressWarnings("unchecked")
	public PojoWorkPlan getCurrentWorkPlan(SessionImplementor sessionImplementor) {
		SearchSessionImplementor searchSession = getSearchSession( sessionImplementor );
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
				AutomaticIndexingSynchronizationStrategy synchronizationStrategy =
						searchSession.getAutomaticIndexingSynchronizationStrategy();
				workPlan = searchSession.createWorkPlan(
						synchronizationStrategy.getDocumentCommitStrategy(),
						synchronizationStrategy.getDocumentRefreshStrategy()
				);
				workPlanPerTransaction.put( transactionIdentifier, workPlan );
				Synchronization txSync = createTransactionWorkQueueSynchronization(
						workPlan, workPlanPerTransaction, transactionIdentifier, synchronizationStrategy
				);
				registerSynchronization( sessionImplementor, txSync );
			}
			return workPlan;
		}
		/*
		 * TODO handle the "simulated" transaction when "a Flush listener is registered".
		 * See:
		 *  - HibernateSearchEventListener (in Search 5 and here)
		 *  - the else block in org.hibernate.search.event.impl.EventSourceTransactionContext#registerSynchronization in Search 5
		else if ( some condition ) {
			throw new UnsupportedOperationException( "Not implemented yet" );
		}
		 */
		else {
			// TODO add a warning when configuration expects transactions, but none was found
//			if ( transactionExpected ) {
//				// this is a workaround: isTransactionInProgress should return "true"
//				// for correct configurations.
//				log.pushedChangesOutOfTransaction();
//			}
			// TODO Create a work plan (to handle automatic reindexing of containing types),
			// but ensure changes will be applied without waiting for a call to workPlan.execute()
			// TODO also ensure synchronicity if necessary (make some Session event, such as flush(), wait for the works to be executed)
			throw new UnsupportedOperationException( "Not implemented yet" );
		}
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

	private static class SearchSessionClosingListener extends BaseSessionEventListener {
		private final SessionImplementor sessionImplementor;

		private SearchSessionClosingListener(SessionImplementor sessionImplementor) {
			this.sessionImplementor = sessionImplementor;
		}

		@Override
		public void end() {
			@SuppressWarnings("unchecked") // This key "belongs" to us, we know what we put in there.
			TransientReference<SearchSessionImplementor> reference =
					(TransientReference<SearchSessionImplementor>) sessionImplementor.getProperties().get(
							SEARCH_SESSION_KEY );
			SearchSessionImplementor searchSession = reference == null ? null : reference.get();
			if ( searchSession != null ) {
				searchSession.close();
			}
		}
	}

	/**
	 * An adapter for synchronizations, allowing to register them as
	 * {@link BeforeTransactionCompletionProcess} or {@link AfterTransactionCompletionProcess} too,
	 * without running the risk of executing their methods twice.
	 * <p>
	 * Also, suppresses any call to {@link Synchronization#afterCompletion(int)} so that
	 * it can be executed later, in {@link AfterTransactionCompletionProcess#doAfterTransactionCompletion(boolean, SharedSessionContractImplementor)}.
	 *
	 * @see HibernateSearchContextService#registerSynchronization(SessionImplementor, Synchronization)
	 */
	private static class SynchronizationAdapter implements Synchronization,
			BeforeTransactionCompletionProcess, AfterTransactionCompletionProcess {

		private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

		private final Synchronization delegate;
		private boolean beforeExecuted = false;
		private boolean afterExecuted = false;

		SynchronizationAdapter(Synchronization delegate) {
			this.delegate = delegate;
		}

		@Override
		public void beforeCompletion() {
			doBeforeCompletion();
		}

		@Override
		public void afterCompletion(int status) {
			log.tracef(
					"Transaction's afterCompletion is expected to be executed"
					+ " through the AfterTransactionCompletionProcess interface, ignoring: %s", delegate
			);
		}

		@Override
		public void doBeforeTransactionCompletion(SessionImplementor sessionImplementor) {
			try {
				doBeforeCompletion();
			}
			catch (Exception e) {
				throw new HibernateException( "Error while indexing in Hibernate Search (before transaction completion)", e );
			}
		}
		@Override
		public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor sessionImplementor) {
			try {
				doAfterCompletion( success ? Status.STATUS_COMMITTED : Status.STATUS_ROLLEDBACK );
			}
			catch (Exception e) {
				throw new HibernateException( "Error while indexing in Hibernate Search (after transaction completion)", e );
			}
		}

		private void doBeforeCompletion() {
			if ( beforeExecuted ) {
				log.tracef(
						"Transaction's beforeCompletion() phase already been processed, ignoring: %s", delegate
				);
			}
			else {
				delegate.beforeCompletion();
				beforeExecuted = true;
			}
		}

		private void doAfterCompletion(int status) {
			if ( afterExecuted ) {
				log.tracef(
						"Transaction's afterCompletion() phase already been processed, ignoring: %s", delegate
				);
			}
			else {
				delegate.afterCompletion( status );
				afterExecuted = true;
			}
		}
	}
}
