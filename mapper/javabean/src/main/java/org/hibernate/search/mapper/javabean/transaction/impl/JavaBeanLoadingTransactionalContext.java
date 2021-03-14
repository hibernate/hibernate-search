/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.transaction.impl;

import java.lang.invoke.MethodHandles;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import org.hibernate.search.mapper.javabean.log.impl.Log;

import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Valueholder for the services needed by the massindexer to wrap operations in transactions.
 *
 * @since 4.4
 * @author Sanne Grinovero
 */
public class JavaBeanLoadingTransactionalContext {
	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	final TransactionManager transactionManager;

	public JavaBeanLoadingTransactionalContext(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public TransactionManager transactionManager() {
		return transactionManager;
	}

	public boolean wrapInTransaction() {
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

	public boolean transactionInProgress() {
		if ( transactionManager == null ) {
			//no TM, nothing to do OR configuration mistake
			log.trace( "No TransactionManager found, do not start a surrounding JTA transaction" );
			return false;
		}
		try {
			if ( transactionManager.getStatus() == Status.STATUS_ACTIVE ) {
				return true;
			}
		}
		catch (SystemException e) {
			log.cannotGuessTransactionStatus( e );
			return false;
		}
		log.trace( "Transaction in activer state, no need to start a JTA transaction" );
		return false;
	}
}
