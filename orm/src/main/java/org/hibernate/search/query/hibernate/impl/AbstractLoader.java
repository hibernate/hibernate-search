/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.query.hibernate.impl;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
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

	public void init(Session session, SearchFactoryImplementor searchFactoryImplementor) {
		statisticsImplementor = searchFactoryImplementor.getStatisticsImplementor();
		takeTimings = searchFactoryImplementor.getStatistics().isStatisticsEnabled();
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

	public abstract Object executeLoad(EntityInfo entityInfo);

	@Override
	public List load(EntityInfo... entityInfos) {
		long startTime = 0;
		if ( takeTimings ) {
			startTime = System.nanoTime();
		}

		List loadedObjects = executeLoad( entityInfos );

		if ( takeTimings ) {
			statisticsImplementor.objectLoadExecuted( loadedObjects.size(), System.nanoTime() - startTime );
		}
		return loadedObjects;
	}

	public abstract List executeLoad(EntityInfo... entityInfo);
}


