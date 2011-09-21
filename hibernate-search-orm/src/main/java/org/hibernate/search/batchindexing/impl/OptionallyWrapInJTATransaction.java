package org.hibernate.search.batchindexing.impl;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.service.jta.platform.spi.JtaPlatform;

/**
 * Wrap the subsequent Runnable in a JTA Transaction if necessary:
 *  - if the existing Hibernate Core transaction strategy requires a TransactionManager
 *  - if no JTA transaction is already started
 *
 * Unfortunately at this time we need to have access to SessionFactoryImplementor
 *
 * @author Emmanuel Bernard
 */
public class OptionallyWrapInJTATransaction implements Runnable {

	private static final Log log = LoggerFactory.make();

	private final SessionFactoryImplementor factory;
	private final SessionAwareRunnable sessionAwareRunnable;
	private final StatelessSessionAwareRunnable statelessSessionAwareRunnable;

	public OptionallyWrapInJTATransaction(SessionFactory factory, SessionAwareRunnable sessionAwareRunnable) {
		/*
		 * Unfortunately we need to access SessionFactoryImplementor to detect:
		 *  - whether or not we need to start the JTA transaction
		 *  - start it
		 */
		//TODO get SessionFactoryImplementor it from the SearchFactory as we might get a hold of the SFI at startup time
		// if that's the case, SearchFactory should expose something like T unwrap(Class<T> clazz);
		this.factory = (SessionFactoryImplementor) factory;
		this.sessionAwareRunnable = sessionAwareRunnable;
		this.statelessSessionAwareRunnable = null;
	}

	public OptionallyWrapInJTATransaction(SessionFactory factory, StatelessSessionAwareRunnable statelessSessionAwareRunnable) {
		/*
		 * Unfortunately we need to access SessionFactoryImplementor to detect:
		 *  - whether or not we need to start the JTA transaction
		 *  - start it
		 */
		//TODO get SessionFactoryImplementor it from the SearchFactory as we might get a hold of the SFI at startup time
		// if that's the case, SearchFactory should expose something like T unwrap(Class<T> clazz);
		this.factory = (SessionFactoryImplementor) factory;
		this.sessionAwareRunnable = null;
		this.statelessSessionAwareRunnable = statelessSessionAwareRunnable;
	}

	public void run() {
		final boolean wrapInTransaction = wrapInTransaction();
		if ( wrapInTransaction ) {
			TransactionManager transactionManager = getTransactionManager();
			try {
				final Session session;
				final StatelessSession statelessSession;
				if ( sessionAwareRunnable != null ) {
					session = factory.openSession();
					statelessSession = null;
				}
				else {
					session = null;
					statelessSession = factory.openStatelessSession();
				}

				transactionManager.begin();

				if ( sessionAwareRunnable != null ) {
					sessionAwareRunnable.run( session );
				}
				else {
					statelessSessionAwareRunnable.run( statelessSession );
				}

				transactionManager.commit();

				if ( sessionAwareRunnable != null ) {
					session.close();
				}
				else {
					statelessSession.close();
				}
			}
			catch (Throwable e) {
				//TODO exception handling seems messy-ish
				log.errorExecutingRunnableInTransaction( e );
				try {
					transactionManager.rollback();
				}
				catch ( SystemException e1 ) {
					// we already have an exception, don't propagate this one
					log.errorRollbackingTransaction( e.getMessage(), e1 );
				}
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

    private TransactionManager getTransactionManager() {
        return factory.getServiceRegistry().getService(JtaPlatform.class).retrieveTransactionManager();
    }

    boolean wrapInTransaction() {
		final TransactionFactory transactionFactory = factory.getServiceRegistry().getService(TransactionFactory.class);
		if ( !transactionFactory.compatibleWithJtaSynchronization() ) {
			//Today we only require a TransactionManager on JTA based transaction factories
			log.trace( "TransactionFactory does not require a TransactionManager: don't wrap in a JTA transaction" );
			return false;
		}
		final TransactionManager transactionManager = getTransactionManager();
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
		catch ( SystemException e ) {
			log.cannotGuessTransactionStatus( e );
			return false;
		}
		log.trace( "Transaction in progress, no needs to start a JTA transaction" );
		return false;
	}
}
