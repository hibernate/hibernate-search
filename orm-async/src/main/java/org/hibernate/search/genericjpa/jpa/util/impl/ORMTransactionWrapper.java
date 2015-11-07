/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.jpa.util.impl;


import org.hibernate.Transaction;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Created by Martin on 12.11.2015.
 */
public final class ORMTransactionWrapper implements TransactionWrapper {

	private static final Log log = LoggerFactory.make();

	private final Transaction transaction;

	public ORMTransactionWrapper(Transaction transaction) {
		this.transaction = transaction;
	}

	@Override
	public void begin() {
		this.transaction.begin();
	}

	@Override
	public void commit() {
		this.transaction.commit();
	}

	@Override
	public void commitIgnoreExceptions() {
		try {
			this.transaction.commit();
		}
		catch (Exception e) {
			log.exceptionOccurred( "Exception ignored", e );
		}
	}

	@Override
	public void rollback() {
		this.transaction.rollback();
	}

	@Override
	public void setIgnoreExceptionsForJTATransaction(boolean ignoreExceptionsForJTATransaction) {
		//no-op
	}

}
