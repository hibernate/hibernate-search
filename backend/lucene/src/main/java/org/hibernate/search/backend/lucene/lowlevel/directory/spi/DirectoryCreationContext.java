/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.directory.spi;

import java.util.Optional;

import org.hibernate.search.util.common.reporting.EventContext;

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
	 * @return The identifier of the index shard, if relevant.
	 */
	Optional<String> getShardId();

}
