/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.entity;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionManager;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.search.genericjpa.exception.SearchException;
import org.hibernate.search.genericjpa.jpa.util.impl.JPATransactionWrapper;

/**
 * Created by Martin on 08.07.2015.
 */
public final class EntityManagerEntityProviderAdapter {

	private EntityManagerEntityProviderAdapter() {
		//can't touch this!
	}

	public static EntityProvider adapt(
			Class<? extends EntityManagerEntityProvider> providerClass,
			EntityManager em,
			TransactionManager transactionManager) {
		try {
			return new EntityManagerAdapterProvider( providerClass.newInstance(), em, transactionManager, true );
		}
		catch (Exception e) {
			throw new SearchException( e );
		}
	}

	public static EntityProvider adapt(
			Class<? extends EntityManagerEntityProvider> providerClass,
			EntityManagerFactory em,
			TransactionManager transactionManager, int concurrentProviders) {
		try {
			return new EntityManagerFactoryAdapterProvider(
					providerClass.newInstance(),
					em,
					transactionManager,
					true,
					concurrentProviders
			);
		}
		catch (Exception e) {
			throw new SearchException( e );
		}
	}

	private static class EntityManagerFactoryAdapterProvider implements EntityProvider {

		private final BlockingQueue<EntityManagerAdapterProvider> providers;

		public EntityManagerFactoryAdapterProvider(
				EntityManagerEntityProvider provider,
				EntityManagerFactory emf,
				TransactionManager transactionManager,
				boolean wrapInTransaction,
				int count) {
			this.providers = new ArrayBlockingQueue<>( count );
			for ( int i = 0; i < count; ++i ) {
				this.providers.add(
						new EntityManagerAdapterProvider(
								provider,
								emf.createEntityManager(),
								transactionManager,
								wrapInTransaction
						)
				);
			}
		}

		@Override
		public Object get(Class<?> entityClass, Object id, Map<String, Object> hints) {
			EntityManagerAdapterProvider prov = this.providers.poll();
			try {
				return prov.get( entityClass, id, hints );
			}
			finally {
				this.providers.add( prov );
			}
		}

		@Override
		public List getBatch(Class<?> entityClass, List<Object> id, Map<String, Object> hints) {
			EntityManagerAdapterProvider prov = this.providers.poll();
			try {
				return prov.getBatch( entityClass, id, hints );
			}
			finally {
				this.providers.add( prov );
			}
		}

		@Override
		public void close() throws IOException {
			for ( EntityManagerAdapterProvider prov : this.providers ) {
				prov.close();
			}
		}
	}

	private static class EntityManagerAdapterProvider implements EntityProvider {
		private final EntityManagerEntityProvider provider;
		private final EntityManager em;
		private final TransactionManager transactionManager;
		private final boolean wrapInTransaction;
		private final Lock lock = new ReentrantLock();

		public EntityManagerAdapterProvider(
				EntityManagerEntityProvider provider,
				EntityManager em,
				TransactionManager transactionManager,
				boolean wrapInTransaction) {
			this.provider = provider;
			this.em = em;
			this.transactionManager = transactionManager;
			this.wrapInTransaction = wrapInTransaction;
		}

		@Override
		public Object get(Class<?> entityClass, Object id, Map<String, Object> hints) {
			this.lock.lock();
			try {
				if ( this.wrapInTransaction ) {
					JPATransactionWrapper tx =
							JPATransactionWrapper.get( this.em, this.transactionManager );
					tx.begin();
					try {
						return this.provider.get( this.em, entityClass, id, hints );
					}
					finally {
						tx.commit();
					}
				}
				else {
					return this.provider.get( this.em, entityClass, id, hints );
				}
			}
			finally {
				this.lock.unlock();
			}
		}

		@Override
		public List getBatch(Class<?> entityClass, List<Object> id, Map<String, Object> hints) {
			this.lock.lock();
			try {
				if ( this.wrapInTransaction ) {
					JPATransactionWrapper tx =
							JPATransactionWrapper.get( this.em, this.transactionManager );
					tx.begin();
					try {
						return this.provider.getBatch( this.em, entityClass, id, hints );
					}
					finally {
						tx.commit();
					}
				}
				else {
					return this.provider.getBatch( this.em, entityClass, id, hints );
				}
			}
			finally {
				this.lock.unlock();
			}
		}

		@Override
		public void close() throws IOException {
			this.em.close();
		}
	}

}
