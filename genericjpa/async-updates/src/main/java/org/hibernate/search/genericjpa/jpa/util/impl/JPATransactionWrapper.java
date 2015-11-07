/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.jpa.util.impl;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.transaction.TransactionManager;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.search.genericjpa.exception.SearchException;

/**
 * <b>internal, don't use this across different threads!</b>
 *
 * @author Martin Braun
 */
public final class JPATransactionWrapper implements TransactionWrapper {

	private static final Logger LOGGER = Logger.getLogger( JPATransactionWrapper.class.getName() );

	private final EntityTransaction tx;
	private final EntityManager em;
	private final TransactionManager transactionManager;
	private boolean ignoreExceptionsForJTATransaction;

	public JPATransactionWrapper(EntityTransaction tx, EntityManager em, TransactionManager transactionManager) {
		this.tx = tx;
		this.em = em;
		this.transactionManager = transactionManager;
	}

	public static JPATransactionWrapper get(EntityManager em, TransactionManager transactionManager) {
		EntityTransaction tx;
		if ( transactionManager == null ) {
			tx = em.getTransaction();
		}
		else {
			tx = null;
		}
		return new JPATransactionWrapper( tx, em, transactionManager );
	}

	public void setIgnoreExceptionsForJTATransaction(boolean ignoreExceptionsForJTATransaction) {
		this.ignoreExceptionsForJTATransaction = ignoreExceptionsForJTATransaction;
	}

	public void begin() {
		if ( this.tx != null ) {
			this.tx.begin();
		}
		else {
			try {
				this.transactionManager.begin();
				this.em.joinTransaction();
			}
			catch (Exception e) {
				if ( !this.ignoreExceptionsForJTATransaction ) {
					throw new SearchException( e );
				}
			}
		}
	}

	public void commit() {
		if ( this.tx != null ) {
			this.tx.commit();
		}
		else {
			try {
				this.transactionManager.commit();
			}
			catch (Exception e) {
				if ( !this.ignoreExceptionsForJTATransaction ) {
					throw new SearchException( e );
				}
			}
		}
	}

	public void commitIgnoreExceptions() {
		try {
			this.commit();
		}
		catch (Exception e) {
			LOGGER.log( Level.WARNING, "Exception", e );
		}
	}

	public void rollback() {
		if ( this.tx != null ) {
			this.tx.rollback();
		}
		else {
			try {
				this.transactionManager.rollback();
			}
			catch (Exception e) {
				if ( !this.ignoreExceptionsForJTATransaction ) {
					throw new SearchException( e );
				}
			}
		}
	}

}
