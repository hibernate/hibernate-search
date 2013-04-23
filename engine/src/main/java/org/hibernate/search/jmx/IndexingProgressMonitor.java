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
package org.hibernate.search.jmx;

import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.search.jmx.impl.JMXRegistrar;
import org.hibernate.search.util.logging.impl.Log;

import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * A JMX based mass indexer progress monitor. This monitor will allow you to follow mass indexing progress via JMX.
 *
 * @author Hardy Ferentschik
 */
public class IndexingProgressMonitor implements IndexingProgressMonitorMBean, MassIndexerProgressMonitor {
	private static final Log log = LoggerFactory.make();

	private final AtomicLong documentsDoneCounter = new AtomicLong();
	private final AtomicLong documentsBuiltCounter = new AtomicLong();
	private final AtomicLong totalCounter = new AtomicLong();
	private final AtomicLong entitiesLoadedCounter = new AtomicLong();

	private final String registeredName;

	public IndexingProgressMonitor() {
		String name = IndexingProgressMonitorMBean.INDEXING_PROGRESS_MONITOR_MBEAN_OBJECT_NAME;
		if ( JMXRegistrar.isNameRegistered( name ) ) {
			name = name + "@" + Integer.toHexString( hashCode() ); // make the name unique in case there are multiple mass indexers at the same time
		}
		registeredName = JMXRegistrar.registerMBean( this, name );
	}

	public final void documentsAdded(long increment) {
		documentsDoneCounter.addAndGet( increment );
	}

	public final void documentsBuilt(int number) {
		documentsBuiltCounter.addAndGet( number );
	}

	public final void entitiesLoaded(int size) {
		entitiesLoadedCounter.addAndGet( size );
	}

	public final void addToTotalCount(long count) {
		totalCounter.addAndGet( count );
	}

	public final void indexingCompleted() {
		log.indexingCompletedAndMBeanUnregistered( totalCounter.get() );
		JMXRegistrar.unRegisterMBean( registeredName );
	}

	public final long getLoadedEntitiesCount() {
		return entitiesLoadedCounter.get();
	}

	public final long getDocumentsAddedCount() {
		return documentsDoneCounter.get();
	}

	public final long getNumberOfEntitiesToIndex() {
		return totalCounter.get();
	}
}


