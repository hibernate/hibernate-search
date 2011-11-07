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
package org.hibernate.search.batchindexing;

import org.hibernate.search.backend.IndexingMonitor;

/**
 * As a MassIndexer can take some time to finish it's job,
 * a MassIndexerProgressMonitor can be defined in the configuration
 * property hibernate.search.worker.indexing.monitor
 * implementing this interface to track indexing performance.
 * <p/>
 * Implementations must:
 * <ul>
 * <li>	be threadsafe </li>
 * <li> have a no-arg constructor </li>
 * </ul>
 *
 * @author Sanne Grinovero
 * @author Hardy Ferentschik
 */
public interface MassIndexerProgressMonitor extends IndexingMonitor {

	/**
	 * The number of Documents built;
	 * This is invoked several times and concurrently during
	 * the indexing process.
	 *
	 * @param number number of {@code Document}s built
	 */
	void documentsBuilt(int number);

	/**
	 * The number of entities loaded from database;
	 * This is invoked several times and concurrently during
	 * the indexing process.
	 *
	 * @param size number of entities loaded from database
	 */
	void entitiesLoaded(int size);

	/**
	 * The total count of entities to be indexed is
	 * added here; It could be called more than once,
	 * the implementation should add them up.
	 * This is invoked several times and concurrently during
	 * the indexing process.
	 *
	 * @param count number of newly indexed entities which has to
	 * be added to total count
	 */
	void addToTotalCount(long count);

	/**
	 * Invoked when the indexing is completed.
	 */
	void indexingCompleted();
}
