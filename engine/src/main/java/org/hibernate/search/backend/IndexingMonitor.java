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
	 * Notify the IndexingMonitor of the number of documents added to the index.
	 * This can be invoked several times during the indexing process, and could
	 * be invoked concurrently by different threads.
	 *
	 * @param increment number of documents add operations performed
	 */
	void documentsAdded(long increment);

}
