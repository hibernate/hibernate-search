/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import java.util.function.BiFunction;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.hibernate.search.util.common.impl.Closer;

class JPAPersistenceRunner implements PersistenceRunner<EntityManager, EntityTransaction> {
	private final EntityManagerFactory entityManagerFactory;

	JPAPersistenceRunner(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;
	}

	@Override
	public <R> R applyNoTransaction(Function<? super EntityManager, R> action) {
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
	public <R> R apply(BiFunction<? super EntityManager, ? super EntityTransaction, R> action) {
		return applyNoTransaction( entityManager -> {
			return OrmUtils.withinJPATransaction( entityManager, tx -> { return action.apply( entityManager, tx ); } );
		} );
	}
}
