/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.jpa.util.impl;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

/**
 * Created by Martin on 11.11.2015.
 */
public final class JPAEntityManagerWrapper implements EntityManagerWrapper {

	private final EntityManager em;
	private final TransactionWrapper tx;

	public JPAEntityManagerWrapper(EntityManager em, TransactionManager transactionManager) {
		this.em = em;
		this.tx = JPATransactionWrapper.get( em, transactionManager );
	}

	@Override
	public QueryWrapper createQuery(String jpqlQuery) {
		return new JPAQueryWrapper( this.em.createQuery( jpqlQuery ) );
	}

	@Override
	public QueryWrapper createNativeQuery(String sqlQuery) {
		return new JPAQueryWrapper( this.em.createNativeQuery( sqlQuery ) );
	}

	@Override
	public TransactionWrapper getTransaction() {
		return this.tx;
	}

	@Override
	public void close() {
		this.em.close();
	}

	@Override
	public void clear() {
		this.em.clear();
	}

	@Override
	public void flush() {
		this.em.flush();
	}
}
