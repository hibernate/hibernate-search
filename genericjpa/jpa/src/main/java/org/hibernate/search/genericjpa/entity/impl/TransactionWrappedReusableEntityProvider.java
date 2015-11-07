/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.entity.impl;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.search.genericjpa.entity.ReusableEntityProvider;
import org.hibernate.search.genericjpa.exception.AssertionFailure;
import org.hibernate.search.genericjpa.exception.SearchException;

/**
 * automatically rolls back the transaction on close
 */
public abstract class TransactionWrappedReusableEntityProvider implements ReusableEntityProvider {

	private final EntityManagerFactory emf;
	private final TransactionManager transactionManager;
	private final boolean useJTATransaction;
	private boolean open = false;
	private boolean startedJTA;
	private EntityManager em;

	public TransactionWrappedReusableEntityProvider(
			EntityManagerFactory emf,
			TransactionManager transactionManager) {
		this.emf = emf;
		this.transactionManager = transactionManager;
		this.useJTATransaction = transactionManager != null;
	}

	protected EntityManager getEntityManager() {
		return this.em;
	}

	@Override
	public void close() {
		try {
			if ( !this.open ) {
				throw new IllegalStateException( "already closed!" );
			}
			this.rollbackTransaction();
			this.em.close();
		}
		finally {
			this.open = false;
			this.em = null;
		}
	}

	@Override
	public void open() {
		try {
			if ( this.open ) {
				throw new IllegalStateException( "already open!" );
			}
			this.em = this.emf.createEntityManager();
			this.beginTransaction();
			this.open = true;
		}
		catch (Throwable e) {
			if ( this.em != null ) {
				this.em.close();
			}
			this.em = null;
			throw e;
		}
	}

	public boolean isOpen() {
		return this.open;
	}

	private void beginTransaction() {
		if ( !this.useJTATransaction ) {
			this.em.getTransaction().begin();
		}
		else {
			try {
				if ( this.transactionManager.getStatus() == Status.STATUS_NO_TRANSACTION ) {
					this.transactionManager.begin();
					this.em.joinTransaction();
					this.startedJTA = true;
				}
				else {
					throw new AssertionFailure(
							"TransactionWrappedReusableEntityProvider must be able to start/close it's own transactions!"
					);
				}
			}
			catch (NotSupportedException | SystemException e) {
				throw new SearchException( "couldn't start a JTA Transaction", e );
			}
		}
	}

	/**
	 * this class should only be used in a read only way
	 */
	private void rollbackTransaction() {
		if ( !this.useJTATransaction ) {
			this.em.getTransaction().rollback();
		}
		else {
			try {
				//well if we didnt start the transaction
				//we cannot rollback properly, but this shouldn't happen
				if ( this.startedJTA ) {
					this.startedJTA = false;
					this.transactionManager.rollback();
				}
				else {
					throw new AssertionFailure(
							"TransactionWrappedReusableEntityProvider must be able to start/close it's own transactions!"
					);
				}
			}
			catch (SecurityException | IllegalStateException | SystemException e) {
				throw new SearchException( "couldn't commit a JTA Transaction", e );
			}
		}
	}
}
