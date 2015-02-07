/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batchindexing.impl;

import javax.transaction.SystemException;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Wraps the execution of a {@code Runnable} in a JTA Transaction if necessary:
 * <ul>
 * <li>if the existing Hibernate Core transaction strategy requires a TransactionManager</li>
 * <li>if no JTA transaction is already started</li>
 * </ul>
 *
 * Unfortunately at this time we need to have access to {@code SessionFactoryImplementor}.
 *
 * @author Emmanuel Bernard
 */
public class OptionallyWrapInJTATransaction extends ErrorHandledRunnable {

	private static final Log log = LoggerFactory.make();

	private final SessionAwareRunnable sessionAwareRunnable;
	private final StatelessSessionAwareRunnable statelessSessionAwareRunnable;
	private final BatchTransactionalContext batchContext;
	private final boolean wrapInTransaction;
	private final String tenantId;

	public OptionallyWrapInJTATransaction(BatchTransactionalContext batchContext, SessionAwareRunnable sessionAwareRunnable, String tenantId) {
		super( batchContext.extendedIntegrator );
		/*
		 * Unfortunately we need to access SessionFactoryImplementor to detect:
		 *  - whether or not we need to start the JTA transaction
		 *  - start it
		 */
		this.batchContext = batchContext;
		this.sessionAwareRunnable = sessionAwareRunnable;
		this.tenantId = tenantId;
		this.statelessSessionAwareRunnable = null;
		this.wrapInTransaction = batchContext.wrapInTransaction();
	}

	public OptionallyWrapInJTATransaction(BatchTransactionalContext batchContext, StatelessSessionAwareRunnable statelessSessionAwareRunnable, String tenantId) {
		super( batchContext.extendedIntegrator );
		/*
		 * Unfortunately we need to access SessionFactoryImplementor to detect:
		 *  - whether or not we need to start the JTA transaction
		 *  - start it
		 */
		this.batchContext = batchContext;
		this.sessionAwareRunnable = null;
		this.tenantId = tenantId;
		this.statelessSessionAwareRunnable = statelessSessionAwareRunnable;
		this.wrapInTransaction = batchContext.wrapInTransaction();
	}

	@Override
	public void runWithErrorHandler() throws Exception {
		if ( wrapInTransaction ) {
			final Session session;
			final StatelessSession statelessSession;
			if ( sessionAwareRunnable != null ) {
				if ( tenantId == null ) {
					session = batchContext.factory.openSession();
				}
				else {
					session = batchContext.factory.withOptions().tenantIdentifier( tenantId ).openSession();
				}
				statelessSession = null;
			}
			else {
				session = null;
				statelessSession = batchContext.factory.withStatelessOptions().tenantIdentifier( tenantId ).openStatelessSession();
			}

			batchContext.transactionManager.begin();

			if ( sessionAwareRunnable != null ) {
				sessionAwareRunnable.run( session );
			}
			else {
				statelessSessionAwareRunnable.run( statelessSession );
			}

			batchContext.transactionManager.commit();

			if ( sessionAwareRunnable != null ) {
				session.close();
			}
			else {
				statelessSession.close();
			}
		}
		else {
			if ( sessionAwareRunnable != null ) {
				sessionAwareRunnable.run( null );
			}
			else {
				statelessSessionAwareRunnable.run( null );
			}
		}
	}

	@Override
	protected void cleanUpOnError() {
		if ( wrapInTransaction ) {
			try {
				batchContext.transactionManager.rollback();
			}
			catch (SystemException e) {
				log.errorRollingBackTransaction( e.getMessage(), e );
			}
		}
	}

}
