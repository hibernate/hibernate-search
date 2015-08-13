/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * @return the total search time in nanoseconds.
	 */
	long getSearchQueryTotalTime();

	/**
	 * Get the time in nanoseconds of the slowest search.
	 * @return the time in nanoseconds of the slowest search.
	 */
	long getSearchQueryExecutionMaxTime();

	/**
	 * Get the average search time in nanoseconds.
	 * @return the average search time in nanoseconds
	 */
	long getSearchQueryExecutionAvgTime();

	/**
	 * Get the query string for the slowest query.
	 * @return the query string for the slowest query.
	 */
	String getSearchQueryExecutionMaxTimeQueryString();

	/**
	 * @return the total object loading in nanoseconds.
	 */
	long getObjectLoadingTotalTime();

	/**
	 * @return the time in nanoseconds for the slowest object load.
	 */
	long getObjectLoadingExecutionMaxTime();

	/**
	 * @return the average object loading time in nanoseconds.
	 */
	long getObjectLoadingExecutionAvgTime();

	/**
	 * @return the total number of objects loaded
	 */
	long getObjectsLoadedCount();

	/**
	 * @return {@code true} if statistics are enabled, {@code false} otherwise
	 */
	boolean isStatisticsEnabled();

	/**
	 * Enable statistics logs (this is a dynamic parameter)
	 * @param b if {@code true}, it enables the statistics.
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
