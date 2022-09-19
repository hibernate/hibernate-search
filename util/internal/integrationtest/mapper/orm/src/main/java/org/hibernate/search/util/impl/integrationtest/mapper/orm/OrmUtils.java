/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.hibernate.search.util.impl.test.function.ThrowingConsumer;
import org.hibernate.search.util.impl.test.function.ThrowingFunction;

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

	public static <E extends Throwable> void runInTransaction(Session session, ThrowingConsumer<Transaction, E> action) throws E {
		applyInTransaction( session, tx -> {
			action.accept( tx );
			return null;
		} );
	}

	public static <R, E extends Throwable> R applyInTransaction(Session session, ThrowingFunction<Transaction, R, E> action) throws E {
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

	public static <R, E extends Throwable> R applyInJPATransaction(EntityManager entityManager, ThrowingFunction<EntityTransaction, R, E> action) throws E {
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
