/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.db.events.eclipselink.impl;

import javax.persistence.EntityManagerFactory;
import javax.transaction.TransactionManager;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.sessions.Session;

import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.genericjpa.JPASearchFactoryController;
import org.hibernate.search.genericjpa.db.events.index.impl.IndexUpdater;
import org.hibernate.search.genericjpa.entity.ReusableEntityProvider;
import org.hibernate.search.genericjpa.events.impl.SynchronizedUpdateSource;
import org.hibernate.search.genericjpa.impl.JPASearchFactoryAdapter;
import org.hibernate.search.genericjpa.impl.SynchronizedUpdateSourceProvider;
import org.hibernate.search.genericjpa.metadata.impl.RehashedTypeMetadata;

/**
 * Created by Martin on 27.07.2015.
 */
public class EclipseLinkSynchronizedUpdateSourceProvider implements SynchronizedUpdateSourceProvider {

	@Override
	public SynchronizedUpdateSource getUpdateSource(
			ExtendedSearchIntegrator searchIntegrator,
			Map<Class<?>, RehashedTypeMetadata> rehashedTypeMetadataPerIndexRoot,
			Map<Class<?>, List<Class<?>>> containedInIndexOf,
			Properties properties,
			EntityManagerFactory emf,
			TransactionManager transactionManager,
			Set<Class<?>> indexRelevantEntities) {
		JpaEntityManager entityManager = (JpaEntityManager) emf.createEntityManager();
		try {
			Session session = entityManager.getServerSession();

			IndexUpdater indexUpdater = new IndexUpdater(
					rehashedTypeMetadataPerIndexRoot,
					containedInIndexOf,
					new DummyReusableEntityProvider(),
					searchIntegrator
			);

			EclipseLinkUpdateSource eclipseLinkUpdateSource = new EclipseLinkUpdateSource(
					indexUpdater,
					indexRelevantEntities,
					rehashedTypeMetadataPerIndexRoot,
					containedInIndexOf,
					transactionManager
			);
			for ( Class<?> entity : indexRelevantEntities ) {
				if ( session.getDescriptor( entity ) == null ) {
					//no JPA entity
					continue;
				}
				session.getDescriptor( entity )
						.getDescriptorEventManager()
						.addListener( eclipseLinkUpdateSource.descriptorEventAspect );
			}
			if(transactionManager == null) {
				//equivalent to resource local tx
				session.getEventManager().addListener( eclipseLinkUpdateSource.sessionEventAspect );
			}
			return eclipseLinkUpdateSource;
		}
		finally {
			entityManager.close();
		}
	}
}
