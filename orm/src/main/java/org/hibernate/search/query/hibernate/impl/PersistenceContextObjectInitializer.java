/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class PersistenceContextObjectInitializer implements ObjectInitializer {
	private static final Log log = LoggerFactory.make();
	private final ObjectInitializer delegate;

	public PersistenceContextObjectInitializer(ObjectInitializer delegate) {
		this.delegate = delegate;
	}

	@Override
	public void initializeObjects(EntityInfo[] entityInfos,
								  Criteria criteria,
								  Class<?> entityType,
								  SearchFactoryImplementor searchFactoryImplementor,
								  TimeoutManager timeoutManager,
								  Session session) {
		//Do not call isTimeOut here as the caller might be the last biggie on the list.
		final int maxResults = entityInfos.length;
		if ( maxResults == 0 ) {
			if ( log.isTraceEnabled() ) {
				log.tracef( "No object to initialize", maxResults );
			}
			return;
		}

		SessionImplementor sessionImplementor = (SessionImplementor) session;
		String entityName = session.getSessionFactory().getClassMetadata( entityType ).getEntityName();
		EntityPersister persister = sessionImplementor.getFactory().getEntityPersister( entityName );
		PersistenceContext persistenceContext = sessionImplementor.getPersistenceContext();

		//check the persistence context
		List<EntityInfo> remainingEntityInfos = new ArrayList<>( maxResults );
		for ( EntityInfo entityInfo : entityInfos ) {
			if ( ObjectLoaderHelper.areDocIdAndEntityIdIdentical( entityInfo, session ) ) {
				EntityKey entityKey = sessionImplementor.generateEntityKey( entityInfo.getId(), persister );
				final boolean isInitialized = persistenceContext.containsEntity( entityKey );
				if ( !isInitialized ) {
					remainingEntityInfos.add( entityInfo );
				}
			}
			else {
				//if document id !=  entity id we can't use PC lookup
				remainingEntityInfos.add( entityInfo );
			}
		}
		//update entityInfos to only contains the remaining ones
		final int remainingSize = remainingEntityInfos.size();
		if ( log.isTraceEnabled() ) {
			log.tracef( "Initialized %d objects out of %d in the persistence context", maxResults - remainingSize, maxResults );
		}
		if ( remainingSize > 0 ) {
			delegate.initializeObjects(
					remainingEntityInfos.toArray( new EntityInfo[remainingSize] ),
					criteria,
					entityType,
					searchFactoryImplementor,
					timeoutManager,
					session
			);
		}
	}
}
