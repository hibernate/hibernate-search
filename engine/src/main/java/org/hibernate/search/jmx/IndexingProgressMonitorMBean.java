/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jmx;

/**
 * A MBean for following the progress of mass indexing.
 *
 * @author Hardy Ferentschik
 */
public interface IndexingProgressMonitorMBean {

	String INDEXING_PROGRESS_MONITOR_MBEAN_OBJECT_NAME = "org.hibernate.search.jmx:type=IndexingProgressMBean";

	/**
	 * @return the number of entities loaded so far
	 */
	long getLoadedEntitiesCount();

	/**
	 * @return the number of Lucene {@code Document}s added so far
	 */
	long getDocumentsAddedCount();

	/**
	 * @return the total number of entities which need indexing
	 */
	long getNumberOfEntitiesToIndex();

}
