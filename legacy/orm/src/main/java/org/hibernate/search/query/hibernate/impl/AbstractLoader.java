/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.stat.spi.StatisticsImplementor;


/**
 * Abstract loader which will take care of taking object loading timings.
 *
 * @author Hardy Ferentschik
 */
public abstract class AbstractLoader implements Loader {
	private StatisticsImplementor statisticsImplementor;
	private boolean takeTimings;

	public void init(SessionImplementor session, ExtendedSearchIntegrator extendedIntegrator) {
		statisticsImplementor = extendedIntegrator.getStatisticsImplementor();
		takeTimings = extendedIntegrator.getStatistics().isStatisticsEnabled();
	}

	@Override
	public final Object load(EntityInfo entityInfo) {
		long startTime = 0;
		if ( takeTimings ) {
			startTime = System.nanoTime();
		}
		Object loadedObject = executeLoad( entityInfo );
		if ( takeTimings ) {
			statisticsImplementor.objectLoadExecuted( 1, System.nanoTime() - startTime );
		}
		return loadedObject;
	}

	@Override
	public Object loadWithoutTiming(EntityInfo entityInfo) {
		return executeLoad( entityInfo );
	}

	protected abstract Object executeLoad(EntityInfo entityInfo);

	@Override
	public List load(List<EntityInfo> entityInfos) {
		long startTime = 0;
		if ( takeTimings ) {
			startTime = System.nanoTime();
		}

		List loadedObjects;
		if ( entityInfos.isEmpty() ) {
			loadedObjects = Collections.EMPTY_LIST;
		}
		else if ( entityInfos.size() == 1 ) {
			final Object entity = executeLoad( entityInfos.get( 0 ) );
			if ( entity == null ) {
				loadedObjects = Collections.EMPTY_LIST;
			}
			else {
				loadedObjects = Collections.singletonList( entity );
			}
		}
		else {
			loadedObjects = executeLoad( entityInfos );
		}

		if ( takeTimings ) {
			statisticsImplementor.objectLoadExecuted( loadedObjects.size(), System.nanoTime() - startTime );
		}
		return loadedObjects;
	}

	protected abstract List executeLoad(List<EntityInfo> entityInfo);
}


