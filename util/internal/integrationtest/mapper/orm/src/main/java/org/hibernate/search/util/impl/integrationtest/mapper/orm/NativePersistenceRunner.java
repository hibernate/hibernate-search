/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.util.impl.test.function.ThrowingBiFunction;
import org.hibernate.search.util.impl.test.function.ThrowingFunction;

class NativePersistenceRunner implements PersistenceRunner<Session, Transaction> {
	private final SessionFactory sessionFactory;
	private final Object tenantId;

	NativePersistenceRunner(SessionFactory sessionFactory, Object tenantId) {
		this.sessionFactory = sessionFactory;
		this.tenantId = tenantId;
	}

	@Override
	public <R, E extends Throwable> R applyNoTransaction(ThrowingFunction<? super Session, R, E> action) throws E {
		if ( tenantId != null ) {
			try ( Session session = sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession() ) {
				return action.apply( session );
			}
		}
		else {
			try ( Session session = sessionFactory.openSession() ) {
				return action.apply( session );
			}
		}
	}

	@Override
	public <R, E extends Throwable> R applyInTransaction(ThrowingBiFunction<? super Session, ? super Transaction, R, E> action)
			throws E {
		return applyNoTransaction( session ->
		//CHECKSTYLE:OFF: RegexpSinglelineJava - cannot use static import as that would clash with method of this class
		OrmUtils.applyInTransaction( session, tx -> {
			return action.apply( session, tx );
		} )
		//CHECKSTYLE:ON
		);
	}
}
