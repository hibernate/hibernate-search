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
package org.hibernate.search.stat;

import java.util.Map;
import java.util.Set;

/**
 * Interface which defines several methods allowing to access statistical data. This includes average and maximum
 * Lucene query time and object loading time.
 *
 * @author Hardy Ferentschik
 */
public interface Statistics {

	/**
	 * Reset all statistics.
	 */
	void clear();

	/**
	 * Get global number of executed search queries
	 *
	 * @return search query execution count
	 */
	long getSearchQueryExecutionCount();

	/**
	 * Get the total search time in nanoseconds.
	 */
	long getSearchQueryTotalTime();

	/**
	 * Get the time in nanoseconds of the slowest search.
	 */
	long getSearchQueryExecutionMaxTime();

	/**
	 * Get the average search time in nanoseconds.
	 */
	long getSearchQueryExecutionAvgTime();

	/**
	 * Get the query string for the slowest query.
	 */
	String getSearchQueryExecutionMaxTimeQueryString();

	/**
	 * Get the total object loading in nanoseconds.
	 */
	long getObjectLoadingTotalTime();

	/**
	 * Get the time in nanoseconds for the slowest object load.
	 */
	long getObjectLoadingExecutionMaxTime();

	/**
	 * Get the average object loading time in nanoseconds.
	 */
	long getObjectLoadingExecutionAvgTime();

	/**
	 * Gets the total number of objects loaded
	 */
	long getObjectsLoadedCount();

	/**
	 * Are statistics logged
	 */
	boolean isStatisticsEnabled();

	/**
	 * Enable statistics logs (this is a dynamic parameter)
	 */
	void setStatisticsEnabled(boolean b);

	/**
	 * Returns the Hibernate Search version.
	 *
	 * @return the Hibernate Search version
	 */
	String getSearchVersion();

	/**
	 * Returns a list of all indexed classes.
	 *
	 * @return list of all indexed classes
	 */
	Set<String> getIndexedClassNames();

	/**
	 * Returns the number of documents for the given entity.
	 *
	 * @param entity the fqc of the entity
	 * @return number of documents for the specified entity name
	 * @throws java.lang.IllegalArgumentException in case the entity name is not valid
	 */
	int getNumberOfIndexedEntities(String entity);

	/**
	 * Returns a map of all indexed entities and their document count in the index.
	 *
	 * @return a map of all indexed entities and their document count. The map key is the fqc of the entity and
	 *         the map value is the document count.
	 */
	Map<String, Integer> indexedEntitiesCount();
}
