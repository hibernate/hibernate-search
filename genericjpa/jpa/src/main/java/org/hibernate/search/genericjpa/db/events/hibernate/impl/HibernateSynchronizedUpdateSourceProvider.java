/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.db.events.hibernate.impl;

import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionManager;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.genericjpa.events.impl.SynchronizedUpdateSource;
import org.hibernate.search.genericjpa.impl.SynchronizedUpdateSourceProvider;
import org.hibernate.search.genericjpa.metadata.impl.RehashedTypeMetadata;
import org.hibernate.service.ServiceRegistry;

/**
 * Created by Martin on 28.07.2015.
 */
public class HibernateSynchronizedUpdateSourceProvider implements SynchronizedUpdateSourceProvider {

	@Override
	public SynchronizedUpdateSource getUpdateSource(
			ExtendedSearchIntegrator searchIntegrator,
			Map<Class<?>, RehashedTypeMetadata> rehashedTypeMetadataPerIndexRoot,
			Map<Class<?>, List<Class<?>>> containedInIndexOf,
			Properties properties,
			EntityManagerFactory emf,
			TransactionManager transactionManager,
			Set<Class<?>> indexRelevantEntities) {
		HibernateEntityManagerFactory hibernateEntityManagerFactory =
				(HibernateEntityManagerFactory) emf;
		SessionFactoryImpl sessionFactory = (SessionFactoryImpl) hibernateEntityManagerFactory.getSessionFactory();
		ServiceRegistry serviceRegistry = sessionFactory.getServiceRegistry();
		EventListenerRegistry listenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );

		HibernateUpdateSource updateSource = new HibernateUpdateSource();
		updateSource.initialize( searchIntegrator );

		listenerRegistry.addDuplicationStrategy( new DuplicationStrategyImpl( HibernateUpdateSource.class ) );

		listenerRegistry.appendListeners( EventType.POST_INSERT, updateSource );
		listenerRegistry.appendListeners( EventType.POST_UPDATE, updateSource );
		listenerRegistry.appendListeners( EventType.POST_DELETE, updateSource );
		listenerRegistry.appendListeners( EventType.POST_COLLECTION_RECREATE, updateSource );
		listenerRegistry.appendListeners( EventType.POST_COLLECTION_REMOVE, updateSource );
		listenerRegistry.appendListeners( EventType.POST_COLLECTION_UPDATE, updateSource );

		return updateSource;
	}

	public static class DuplicationStrategyImpl implements DuplicationStrategy {
		private final Class checkClass;

		public DuplicationStrategyImpl(Class checkClass) {
			this.checkClass = checkClass;
		}

		@Override
		public boolean areMatch(Object listener, Object original) {
			// not isAssignableFrom since the user could subclass
			return checkClass == original.getClass() && checkClass == listener.getClass();
		}

		@Override
		public DuplicationStrategy.Action getAction() {
			return Action.KEEP_ORIGINAL;
		}
	}

}
