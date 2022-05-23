/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.withinTransaction;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.util.impl.test.function.ThrowingBiFunction;
import org.hibernate.search.util.impl.test.function.ThrowingFunction;

class NativePersistenceRunner implements PersistenceRunner<Session, Transaction> {
	private final SessionFactory sessionFactory;
	private final String tenantId;

	NativePersistenceRunner(SessionFactory sessionFactory, String tenantId) {
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
	public <R, E extends Throwable> R applyInTransaction(ThrowingBiFunction<? super Session, ? super Transaction, R, E> action) throws E {
		return applyNoTransaction( session ->
				withinTransaction( session, tx -> {
					return action.apply( session, tx );
				} )
		);
	}
}
