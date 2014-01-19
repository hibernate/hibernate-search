/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.batchindexing.impl;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.event.spi.EventSource;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
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
	final SearchFactoryImplementor searchFactoryImplementor;

	public BatchTransactionalContext(SearchFactoryImplementor searchFactoryImplementor, SessionFactoryImplementor sessionFactory, ErrorHandler errorHandler) {
		this.searchFactoryImplementor = searchFactoryImplementor;
		this.factory = sessionFactory;
		this.errorHandler = errorHandler;
		this.transactionManager = lookupTransactionManager( factory );
		this.transactionFactory = lookupTransactionFactory( factory );
	}

	private static TransactionFactory<?> lookupTransactionFactory(SessionFactoryImplementor sessionFactory) {
		return sessionFactory.getServiceRegistry().getService( TransactionFactory.class );
	}

	private static TransactionManager lookupTransactionManager(SessionFactoryImplementor sessionFactory) {
		final Session session = sessionFactory.openSession();
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
