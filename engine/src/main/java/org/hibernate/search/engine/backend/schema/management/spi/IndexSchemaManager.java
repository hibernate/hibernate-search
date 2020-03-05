/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.schema.management.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.util.common.SearchException;

/**
 * A manager for the schema of a single index,
 * i.e. the data structure in which index data is stored.
 * It includes the core index files, but also any additional configuration required
 * by the backend such as field metadata or analyzer definitions.
 * <p>
 * A schema has to be created before indexing and searching can happen.
 * <p>
 * A schema can become obsolete, in which case it needs to be updated (not always possible)
 * or re-created (will drop any indexed data).
 */
public interface IndexSchemaManager {

	/**
	 * Creates the schema if it doesn't already exist.
	 * <p>
	 * Does not change or validate anything if the schema already exists.
	 *
	 * @return A future.
	 */
	CompletableFuture<?> createIfMissing();

	/**
	 * Creates the schema if it doesn't already exist,
	 * or updates the existing schema to match requirements expressed by the mapper.
	 * <p>
	 * Updating the schema may be impossible (for example if the type of a field changed).
	 * In this case, the future will ultimately be completed with a {@link SearchException}.
	 *
	 * @return A future.
	 */
	CompletableFuture<?> createOrUpdate();

	/**
	 * Drops the schema and all indexed data if it exists.
	 * <p>
	 * Does not change anything if the schema does not exists.
	 *
	 * @return A future.
	 */
	CompletableFuture<?> dropIfExisting();

	/**
	 * Drops the schema and all indexed data if it exists,
	 * then creates the schema.
	 *
	 * @return A future.
	 */
	CompletableFuture<?> dropAndCreate();

	/**
	 * Validates the existing schema against requirements expressed by the mapper.
	 * <p>
	 * If the schema does not exist, a failure is pushed to the given collector.
	 * <p>
	 * If the index exists and validation happens, validation failures do not trigger an exception,
	 * but instead are pushed to the given collector.
	 *
	 * @param failureCollector A collector for validation failures.
	 * @return A future.
	 */
	CompletableFuture<?> validate(ContextualFailureCollector failureCollector);

}
