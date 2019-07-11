/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.spi;

import java.io.IOException;
import java.util.OptionalInt;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.store.Directory;

public interface DirectoryCreationContext {

	/**
	 * @return The event context to use for exceptions.
	 */
	EventContext getEventContext();

	/**
	 * @return The name of the index in Hibernate Search.
	 */
	String getIndexName();

	/**
	 * @return The identifier of the index shard (0-based), if relevant.
	 */
	OptionalInt getShardId();

	/**
	 * Initialize the Lucene Directory if it isn't already.
	 *
	 * @param directory the Directory to initialize
	 * @throws IOException If an IOException is thrown while initializing the index.
	 * @throws SearchException In case of lock acquisition timeouts, IOException, or if a corrupt index is found
	 */
	void initializeIndexIfNeeded(Directory directory) throws IOException;

}
