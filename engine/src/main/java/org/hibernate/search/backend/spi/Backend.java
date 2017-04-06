/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.spi;

import java.util.Properties;

import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * Interface for different types of queue processors. Implementations need a no-arg constructor.
 *
 * @author Yoann Rodiere
 *
 * @hsearch.experimental This type is under active development. You should be prepared
 * for incompatible changes in future releases.
 */
public interface Backend {

	/**
	 * Used at startup, called once as first method.
	 *
	 * @param properties all configuration properties
	 * @param context context giving access to required meta data
	 */
	default void initialize(Properties properties, WorkerBuildContext context) {
		// Empty default
	}

	/**
	 * Used to shutdown and eventually release resources.
	 * No other method should be used after this one.
	 * <p>
	 * The {@link #createQueueProcessor(IndexManager, WorkerBuildContext) produced queue processors} are
	 * guaranteed to be closed before this is called.
	 */
	default void close() {
		// Empty default
	}

	/**
	 * @return {@code true} if this backend is able to enlist in ongoing transactions.
	 */
	default boolean isTransactional() {
		return false;
	}

	/**
	 * Called exactly once for each index manager using this backend.
	 * @param indexManager the {@link IndexManager} the {@link BackendQueueProcessor} will point to
	 * @param context context giving access to required meta data
	 * @return a {@link BackendQueueProcessor} that will send its works to the given index manager
	 */
	BackendQueueProcessor createQueueProcessor(IndexManager indexManager, WorkerBuildContext context);

}
