/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.function.Consumer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.util.common.impl.Closer;

public final class OrmUtils {

	private OrmUtils() {
	}

	public static void withinSession(SessionFactory sessionFactory, Consumer<? super Session> action) {
		try ( Session session = sessionFactory.openSession() ) {
			action.accept( session );
		}
	}

	public static void withinEntityManager(EntityManagerFactory entityManagerFactory, Consumer<EntityManager> action) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( action::accept, entityManager );
			closer.push( EntityManager::close, entityManager );
		}
	}

	public static void withinTransaction(SessionFactory entityManagerFactory, Consumer<Session> action) {
		withinSession(
				entityManagerFactory,
				entityManager -> withinTransaction( entityManager, tx -> action.accept( entityManager ) )
		);
	}

	public static void withinJPATransaction(EntityManagerFactory entityManagerFactory, Consumer<EntityManager> action) {
		withinEntityManager(
				entityManagerFactory,
				entityManager -> withinJPATransaction( entityManager, tx -> action.accept( entityManager ) )
		);
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
			catch (AssertionError e) {
				// An assertion failed while rolling back...
				// Propagate the assertion failure, but make sure to add some context
				e.addSuppressed( t );
				throw e;
			}
			catch (RuntimeException e) {
				t.addSuppressed( e );
			}
			throw t;
		}
	}

	public static void withinJPATransaction(EntityManager entityManager, Consumer<EntityTransaction> action) {
		EntityTransaction tx = entityManager.getTransaction();
		tx.begin();
		try {
			action.accept( tx );
			tx.commit();
		}
		catch (Throwable t) {
			try {
				tx.rollback();
			}
			catch (AssertionError e) {
				// An assertion failed while rolling back...
				// Propagate the assertion failure, but make sure to add some context
				e.addSuppressed( t );
				throw e;
			}
			catch (RuntimeException e) {
				t.addSuppressed( e );
			}
			throw t;
		}
	}

}
