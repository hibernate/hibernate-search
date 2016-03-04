/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.triggers.impl;

import org.hibernate.Session;
import org.hibernate.search.db.util.impl.EntityManagerWrapper;
import org.hibernate.search.db.util.impl.QueryWrapper;
import org.hibernate.search.db.util.impl.TransactionWrapper;

/**
 * Created by Martin on 12.11.2015.
 */
public final class ORMEntityManagerWrapper implements EntityManagerWrapper {

	private final Session session;
	private final ORMTransactionWrapper transaction;

	public ORMEntityManagerWrapper(Session session) {
		this.session = session;
		this.transaction = new ORMTransactionWrapper( this.session.getTransaction() );
	}

	@Override
	public QueryWrapper createQuery(String jpqlQuery) {
		return new ORMQueryWrapper( this.session.createQuery( jpqlQuery ), null );
	}

	@Override
	public QueryWrapper createNativeQuery(String sqlQuery) {
		return new ORMQueryWrapper( null, this.session.createSQLQuery( sqlQuery ) );
	}

	@Override
	public TransactionWrapper getTransaction() {
		return this.transaction;
	}

	@Override
	public void close() {
		this.session.close();
	}

	@Override
	public void clear() {
		this.session.clear();
	}

	@Override
	public void flush() {
		this.session.flush();
	}

}
