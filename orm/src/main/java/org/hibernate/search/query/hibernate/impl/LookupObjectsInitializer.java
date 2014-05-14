/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.TimeoutManager;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Initializes objects using lookup by it.
 * This approach is useful is a batch size has been set on the entity.
 * Hibernate Session will load objects by batch reducing the number of database roundtrip.
 *
 * Note that the second level cache is naturally first checked in this approach.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class LookupObjectsInitializer implements ObjectsInitializer {

	private static final Log log = LoggerFactory.make();

	public static final LookupObjectsInitializer INSTANCE = new LookupObjectsInitializer();

	private LookupObjectsInitializer() {
		// use INSTANCE instead of constructor
	}

	@Override
	public void initializeObjects(EntityInfo[] entityInfos,
										Criteria criteria, Class<?> entityType,
										SearchFactoryImplementor searchFactoryImplementor,
										TimeoutManager timeoutManager,
										Session session) {
		boolean traceEnabled = log.isTraceEnabled();
		//Do not call isTimeOut here as the caller might be the last biggie on the list.
		final int maxResults = entityInfos.length;
		if ( maxResults == 0 ) {
			if ( traceEnabled ) {
				log.tracef( "No object to initialize", maxResults );
			}
			return;
		}

		//TODO should we do time out check between each object call?
		for ( EntityInfo entityInfo : entityInfos ) {
			ObjectLoaderHelper.load( entityInfo, session );
		}
		if ( traceEnabled ) {
			log.tracef( "Initialized %d objects by lookup method.", maxResults );
		}
	}
}
