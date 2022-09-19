/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.transaction.Synchronization;

import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;

public class HibernateOrmSearchSessionHolder implements Serializable {

	private static final String SESSION_PROPERTY_KEY = "hibernate.search.session";

	private static final Map<Transaction, HibernateOrmSearchSessionHolder> holderPerClosedSessionTransaction =
			new ConcurrentHashMap<>();

	// Public for tests only
	public static int staticMapSize() {
		return holderPerClosedSessionTransaction.size();
	}

	public static HibernateOrmSearchSessionHolder get(SessionImplementor session, boolean createIfMissing) {
		HibernateOrmSearchSessionHolder holder = (HibernateOrmSearchSessionHolder)
				session.getProperties().get( SESSION_PROPERTY_KEY );
		if ( holder != null ) {
			return holder;
		}
		boolean closedSessionAndTransaction = session.isClosed() && session.isTransactionInProgress();
		if ( closedSessionAndTransaction ) {
			// This can happen when using JTA.
			// Only do this as a fallback: if somehow the session holder was added to the session properties
			// before the session was closed, we definitely want to use it and avoid the static map.
			holder = holderPerClosedSessionTransaction.get( session.accessTransaction() );
		}
		if ( holder != null ) {
			return holder;
		}
		if ( !createIfMissing ) {
			return null;
		}
		holder = new HibernateOrmSearchSessionHolder();
		if ( closedSessionAndTransaction ) {
			// This can happen when using JTA.
			Transaction transaction = session.accessTransaction();
			transaction.registerSynchronization( new HolderPerClosedSessionTransactionCleanup( transaction ) );
			holderPerClosedSessionTransaction.put( transaction, holder );
		}
		else {
			session.setProperty( SESSION_PROPERTY_KEY, holder );
		}
		return holder;
	}

	// Everything here should be transient because the holder might get serialized along with a Hibernate ORM session.
	// The Hibernate Search data (indexing plans in particular) will be lost in the process,
	// but that's the best we can do.
	private transient HibernateOrmSearchSession searchSession;
	private transient Map<Transaction, PojoIndexingPlan> planPerTransaction;

	public HibernateOrmSearchSession searchSession() {
		return searchSession;
	}

	public void searchSession(HibernateOrmSearchSession searchSession) {
		this.searchSession = searchSession;
	}

	public PojoIndexingPlan pojoIndexingPlan(Transaction transaction) {
		return planPerTransaction == null ? null : planPerTransaction.get( transaction );
	}

	public void pojoIndexingPlan(Transaction transaction, PojoIndexingPlan plan) {
		if ( planPerTransaction == null ) {
			planPerTransaction = new HashMap<>();
		}
		planPerTransaction.put( transaction, plan );
	}

	public void clear(Transaction transactionIdentifier) {
		if ( planPerTransaction == null ) {
			return;
		}
		planPerTransaction.remove( transactionIdentifier );
	}

	private static class HolderPerClosedSessionTransactionCleanup implements Synchronization {
		private final Transaction transaction;

		public HolderPerClosedSessionTransactionCleanup(Transaction transaction) {
			this.transaction = transaction;
		}

		@Override
		public void beforeCompletion() {
			// Nothing to do
		}

		@Override
		public void afterCompletion(int i) {
			holderPerClosedSessionTransaction.remove( transaction );
		}
	}
}
