/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

class NativePersistenceRunner implements PersistenceRunner<Session, Transaction> {
	private final SessionFactory sessionFactory;

	NativePersistenceRunner(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public <R> R applyNoTransaction(Function<? super Session, R> action) {
		try ( Session session = sessionFactory.openSession() ) {
			return action.apply( session );
		}
	}

	@Override
	public <R> R applyInTransaction(BiFunction<? super Session, ? super Transaction, R> action) {
		return applyNoTransaction( session -> {
			return OrmUtils.withinTransaction( session, tx -> { return action.apply( session, tx ); } );
		} );
	}
}
