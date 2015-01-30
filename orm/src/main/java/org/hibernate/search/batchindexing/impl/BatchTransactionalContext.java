/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batchindexing.impl;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.event.spi.EventSource;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Valueholder for the services needed by the massindexer to wrap operations in transactions.
 *
 * @since 4.4
 * @see OptionallyWrapInJTATransaction
 * @author Sanne Grinovero
 */
public class BatchTransactionalContext {

	private static final Log log = LoggerFactory.make();

	final SessionFactoryImplementor factory;
	final ErrorHandler errorHandler;
	final TransactionManager transactionManager;
	final TransactionFactory<?> transactionFactory;
	final ExtendedSearchIntegrator extendedIntegrator;

	public BatchTransactionalContext(ExtendedSearchIntegrator extendedIntegrator, SessionFactoryImplementor sessionFactory, ErrorHandler errorHandler, String tenantId) {
		this.extendedIntegrator = extendedIntegrator;
		this.factory = sessionFactory;
		this.errorHandler = errorHandler;
		this.transactionManager = lookupTransactionManager( factory, tenantId );
		this.transactionFactory = lookupTransactionFactory( factory );
	}

	private static TransactionFactory<?> lookupTransactionFactory(SessionFactoryImplementor sessionFactory) {
		return sessionFactory.getServiceRegistry().getService( TransactionFactory.class );
	}

	private static TransactionManager lookupTransactionManager(SessionFactoryImplementor sessionFactory, String tenantId) {
		final Session session = tenantId == null
				? sessionFactory.openSession()
				: sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession();
		try {
			EventSource eventSource = (EventSource)session;
			return eventSource
				.getTransactionCoordinator()
				.getTransactionContext()
				.getTransactionEnvironment()
				.getJtaPlatform()
				.retrieveTransactionManager();
		}
		finally {
			session.close();
		}
	}

	boolean wrapInTransaction() {
		if ( !transactionFactory.compatibleWithJtaSynchronization() ) {
			//Today we only require a TransactionManager on JTA based transaction factories
			log.trace( "TransactionFactory does not require a TransactionManager: don't wrap in a JTA transaction" );
			return false;
		}
		if ( transactionManager == null ) {
			//no TM, nothing to do OR configuration mistake
			log.trace( "No TransactionManager found, do not start a surrounding JTA transaction" );
			return false;
		}
		try {
			if ( transactionManager.getStatus() == Status.STATUS_NO_TRANSACTION ) {
				log.trace( "No Transaction in progress, needs to start a JTA transaction" );
				return true;
			}
		}
		catch (SystemException e) {
			log.cannotGuessTransactionStatus( e );
			return false;
		}
		log.trace( "Transaction in progress, no need to start a JTA transaction" );
		return false;
	}

}
