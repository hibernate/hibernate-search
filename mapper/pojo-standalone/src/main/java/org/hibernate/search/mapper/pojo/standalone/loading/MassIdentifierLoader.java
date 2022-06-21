/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.loading;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A loader for mass loading of entity identifiers, used in particular during mass indexing.
 */
@Incubating
public interface MassIdentifierLoader extends AutoCloseable {

	/**
	 * Closes this {@link MassIdentifierLoader}.
	 */
	@Override
	void close();

	/**
	 * @return The total count of identifiers expected to be loaded.
	 */
	long totalCount();

	/**
	 * Loads one batch of identifiers and adds them to the sink,
	 * or calls {@link MassIdentifierSink#complete()}
	 * to notify the caller that there are no more identifiers to load.
	 * <p>
	 * Calls to the sink must be performed synchronously (before this method returns).
	 * @throws InterruptedException If the thread was interrupted while performing I/O operations.
	 * This will lead to aborting mass indexing completely.
	 */
	void loadNext() throws InterruptedException;

}
