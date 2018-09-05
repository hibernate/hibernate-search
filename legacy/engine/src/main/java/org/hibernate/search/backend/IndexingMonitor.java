/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public interface IndexingMonitor {

	/**
	 * Notify the monitor that {@code increment} more documents have been added to the index.
	 * <p>
	 * Summing the numbers passed to this method gives the total
	 * number of documents that have been added to the index so far.
	 * <p>
	 * This method is invoked several times during indexing,
	 * and calls are <strong>incremental</strong>:
	 * calling {@code documentsAdded(3)} and then {@code documentsAdded(1)}
	 * should be understood as "3+1 documents, i.e. 4 documents have been added to the index".
	 * <p>
	 * This method can be invoked from several threads thus implementors are required to be thread-safe.
	 *
	 * @param increment additional number of documents built
	 */
	void documentsAdded(long increment);

}
