/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;
import javax.transaction.SystemException;

import org.hibernate.StatelessSession;
import org.hibernate.search.engine.reporting.ErrorHandler;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

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

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BatchTransactionalContext batchContext;
	private final StatelessSessionAwareRunnable statelessSessionAwareRunnable;
	private final Integer transactionTimeout;
	private final boolean wrapInTransaction;
	private final String tenantId;

	public OptionallyWrapInJTATransaction(BatchTransactionalContext batchContext,
			ErrorHandler errorHandler,
			StatelessSessionAwareRunnable statelessSessionAwareRunnable,
			Integer transactionTimeout, String tenantId) {
		super( errorHandler );
		/*
		 * Unfortunately we need to access SessionFactoryImplementor to detect:
		 *  - whether or not we need to start the JTA transaction
		 *  - start it
		 */
		this.batchContext = batchContext;
		this.transactionTimeout = transactionTimeout;
		this.tenantId = tenantId;
		this.statelessSessionAwareRunnable = statelessSessionAwareRunnable;
		this.wrapInTransaction = batchContext.wrapInTransaction();
	}

	@Override
	public void runWithErrorHandler() throws Exception {
		if ( wrapInTransaction ) {
			try ( StatelessSession statelessSession = batchContext.factory.withStatelessOptions()
					.tenantIdentifier( tenantId )
					.openStatelessSession() ) {
				if ( transactionTimeout != null ) {
					batchContext.transactionManager.setTransactionTimeout( transactionTimeout );
				}
				batchContext.transactionManager.begin();
				statelessSessionAwareRunnable.run( statelessSession );
				batchContext.transactionManager.commit();
			}
		}
		else {
			statelessSessionAwareRunnable.run( null );
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
