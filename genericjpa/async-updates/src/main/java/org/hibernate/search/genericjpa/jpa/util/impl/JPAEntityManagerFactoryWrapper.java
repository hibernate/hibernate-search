/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.jpa.util.impl;

import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionManager;

/**
 * Created by Martin on 11.11.2015.
 */
public final class JPAEntityManagerFactoryWrapper implements EntityManagerFactoryWrapper {

	private final EntityManagerFactory emf;
	private final TransactionManager transactionManager;

	public JPAEntityManagerFactoryWrapper(EntityManagerFactory emf, TransactionManager transactionManager) {
		this.emf = emf;
		this.transactionManager = transactionManager;
	}

	@Override
	public EntityManagerWrapper createEntityManager() {
		return new JPAEntityManagerWrapper( this.emf.createEntityManager(), this.transactionManager );
	}

	@Override
	public boolean isOpen() {
		return this.emf.isOpen();
	}

}
