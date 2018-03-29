/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.orm;

import java.util.function.Consumer;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public final class OrmUtils {

	private OrmUtils() {
	}

	public static void withinTransaction(SessionFactory sessionFactory, Consumer<Session> action) {
		withinSession( sessionFactory, session -> withinTransaction( session, tx -> action.accept( session ) ) );
	}

	public static void withinSession(SessionFactory sessionFactory, Consumer<Session> action) {
		try ( Session session = sessionFactory.openSession() ) {
			action.accept( session );
		}
	}

	public static void withinTransaction(Session session, Consumer<Transaction> action) {
		Transaction tx = session.beginTransaction();
		try {
			action.accept( tx );
			tx.commit();
		}
		catch (Throwable t) {
			try {
				tx.rollback();
			}
			catch (RuntimeException e) {
				t.addSuppressed( e );
			}
			throw t;
		}
	}

}
