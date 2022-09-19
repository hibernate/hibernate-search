/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import static org.hibernate.search.util.impl.integrationtest.mapper.orm.OrmUtils.applyInJPATransaction;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.impl.test.function.ThrowingBiFunction;
import org.hibernate.search.util.impl.test.function.ThrowingFunction;

class JPAPersistenceRunner implements PersistenceRunner<EntityManager, EntityTransaction> {
	private final EntityManagerFactory entityManagerFactory;

	JPAPersistenceRunner(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public <R, E extends Throwable> R applyNoTransaction(ThrowingFunction<? super EntityManager, R, E> action) throws E {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			EntityManager entityManager = entityManagerFactory.createEntityManager();
			try {
				return action.apply( entityManager );
			}
			finally {
				closer.push( EntityManager::close, entityManager );
			}
		}
	}

	@Override
	public <R, E extends Throwable> R applyInTransaction(
			ThrowingBiFunction<? super EntityManager, ? super EntityTransaction, R, E> action)
			throws E {
		return applyNoTransaction(
				entityManager -> applyInJPATransaction( entityManager, tx -> action.apply( entityManager, tx ) )
		);
	}
}
