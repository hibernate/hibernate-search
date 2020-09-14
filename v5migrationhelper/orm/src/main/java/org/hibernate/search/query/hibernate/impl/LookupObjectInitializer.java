/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Initializes objects using lookup by id.
 *
 * This approach is useful if a batch size has been set on the entity. Hibernate Session will load objects by batch
 * reducing the number of database round trips.
 *
 * Note that the second level cache is naturally first checked in this approach.
 *
 * @author Emmanuel Bernard
 */
public class LookupObjectInitializer implements ObjectInitializer {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	public static final LookupObjectInitializer INSTANCE = new LookupObjectInitializer();

	private LookupObjectInitializer() {
		// use INSTANCE instead of constructor
	}

	@Override
	public void initializeObjects(List<EntityInfo> entityInfos, LinkedHashMap<EntityInfoLoadKey, Object> idToObjectMap, ObjectInitializationContext objectInitializationContext) {
		boolean traceEnabled = log.isTraceEnabled();

		// Do not call isTimeOut here as the caller might be the last biggie on the list.
		final int maxResults = entityInfos.size();
		if ( maxResults == 0 ) {
			if ( traceEnabled ) {
				log.tracef( "No object to initialize" );
			}
			return;
		}

		for ( EntityInfo entityInfo : entityInfos ) {
			Object o = ObjectLoaderHelper.load( entityInfo, objectInitializationContext.getSession() );
			if ( o != null ) {
				EntityInfoLoadKey key = new EntityInfoLoadKey( entityInfo.getType().getPojoType(), entityInfo.getId() );
				idToObjectMap.put( key, o );
			}
		}
		if ( traceEnabled ) {
			log.tracef( "Initialized %d objects by lookup method.", (Integer) maxResults );
		}
	}
}
