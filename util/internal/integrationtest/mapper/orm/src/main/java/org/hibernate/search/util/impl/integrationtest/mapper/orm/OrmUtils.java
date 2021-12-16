/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

public final class OrmUtils {

	private OrmUtils() {
	}

	public static PersistenceRunner<EntityManager, EntityTransaction> with(EntityManagerFactory entityManagerFactory) {
		return new JPAPersistenceRunner( entityManagerFactory );
	}

	public static PersistenceRunner<Session, Transaction> with(SessionFactory sessionFactory) {
		return with( sessionFactory, null );
	}

	public static PersistenceRunner<Session, Transaction> with(SessionFactory sessionFactory, String tenantId) {
		return new NativePersistenceRunner( sessionFactory, tenantId );
	}

	public static void withinSession(SessionFactory sessionFactory, Consumer<? super Session> action) {
		with( sessionFactory ).runNoTransaction( action );
	}

	public static void withinEntityManager(EntityManagerFactory entityManagerFactory, Consumer<EntityManager> action) {
		with( entityManagerFactory ).runNoTransaction( action );
	}

	public static void withinTransaction(SessionFactory sessionFactory, Consumer<Session> action) {
		with( sessionFactory ).runInTransaction( action );
	}

	public static void withinTransaction(SessionFactory sessionFactory, String tenantId, Consumer<Session> action) {
		with( sessionFactory, tenantId ).runInTransaction( action );
	}

	public static void withinJPATransaction(EntityManagerFactory entityManagerFactory, Consumer<EntityManager> action) {
		with( entityManagerFactory ).runInTransaction( action );
	}

	public static void withinTransaction(Session session, Consumer<Transaction> action) {
		withinTransaction( session, tx -> {
			action.accept( tx );
			return null;
		} );
	}

	public static <R> R withinTransaction(Session session, Function<Transaction, R> action) {
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			R result = action.apply( tx );
			tx.commit();
			return result;
		}
		catch (Throwable t) {
			if ( tx == null ) {
				throw t;
			}
			try {
				tx.rollback();
			}
			catch (AssertionError e) {
				// An assertion failed while rolling back...
				if ( t instanceof AssertionError ) {
					// The original exception was an assertion error, so it's more important.
					t.addSuppressed( e );
				}
				else {
					// The original exception was not an assertion error, so it's less important.
					// Propagate the assertion error, with the suppressed exception as added context.
					e.addSuppressed( t );
					throw e;
				}
			}
			catch (RuntimeException e) {
				t.addSuppressed( e );
			}
			throw t;
		}
	}

	public static <R> R withinJPATransaction(EntityManager entityManager, Function<EntityTransaction, R> action) {
		EntityTransaction tx = null;
		try {
			tx = entityManager.getTransaction();
			tx.begin();
			R result = action.apply( tx );
			tx.commit();
			return result;
		}
		catch (Throwable t) {
			if ( tx == null ) {
				throw t;
			}
			try {
				tx.rollback();
			}
			catch (AssertionError e) {
				// An assertion failed while rolling back...
				if ( t instanceof AssertionError ) {
					// The original exception was an assertion error, so it's more important.
					t.addSuppressed( e );
				}
				else {
					// The original exception was not an assertion error, so it's less important.
					// Propagate the assertion error, with the suppressed exception as added context.
					e.addSuppressed( t );
					throw e;
				}
			}
			catch (RuntimeException e) {
				t.addSuppressed( e );
			}
			throw t;
		}
	}

	public static Number countAll(EntityManager entityManager, Class<?> entityType) {
		return (Number) entityManager.createQuery( "select count(*) from " + entityType.getName() )
				.getSingleResult();
	}

	public static <T> List<T> listAll(EntityManager entityManager, Class<T> entityType) {
		return queryAll( entityManager, entityType ).getResultList();
	}

	public static <T> Query<T> queryAll(EntityManager entityManager, Class<T> entityType) {
		return entityManager.unwrap( Session.class )
				.createQuery( "select e from " + entityType.getName() + " e", entityType );
	}
}
